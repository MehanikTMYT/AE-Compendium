package ru.mehanik.aecompendium.gadgets;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Server side of "Order from AE": for every missing material, take what the bound AE network already stores into
 * account, then plan and submit a crafting job for the rest. Results are reported in chat once all plans are done.
 */
public final class GadgetOrders {
    private static final int MAX_PENDING_PER_PLAYER = 64;
    private static final List<Order> PENDING = new ArrayList<>();

    private GadgetOrders() {}

    private record Job(AEItemKey key, long amount, Future<ICraftingPlan> plan) {}

    private static final class Order {
        final ServerPlayer player;
        final IGrid grid;
        final IActionSource source;
        final List<Job> jobs = new ArrayList<>();
        final List<Component> lines = new ArrayList<>();
        int submitted;

        Order(ServerPlayer player, IGrid grid) {
            this.player = player;
            this.grid = grid;
            this.source = IActionSource.ofPlayer(player);
        }
    }

    public static void handle(OrderMissingPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        IGrid grid = boundGrid(player);
        if (grid == null) {
            player.sendSystemMessage(Component.translatable("ae_compendium.order.not_bound").withStyle(ChatFormatting.RED));
            return;
        }
        Order order = new Order(player, grid);
        ICraftingSimulationRequester requester = () -> order.source;
        var storage = grid.getStorageService().getInventory();
        var crafting = grid.getCraftingService();
        for (OrderMissingPayload.Entry entry : payload.entries()) {
            if (order.jobs.size() >= MAX_PENDING_PER_PLAYER) break;
            if (entry.amount() <= 0 || entry.item().isEmpty()) continue;
            AEItemKey key = AEItemKey.of(entry.item());
            if (key == null) continue;
            long stored = storage.extract(key, entry.amount(), Actionable.SIMULATE, order.source);
            long toCraft = entry.amount() - stored;
            if (toCraft <= 0) {
                order.lines.add(line("in_network", ChatFormatting.GRAY, key, entry.amount()));
                continue;
            }
            if (!crafting.isCraftable(key)) {
                order.lines.add(line("not_craftable", ChatFormatting.RED, key, toCraft));
                continue;
            }
            order.jobs.add(new Job(key, toCraft, crafting.beginCraftingCalculation(
                player.level(), requester, key, toCraft, CalculationStrategy.REPORT_MISSING_ITEMS)));
        }
        if (order.jobs.isEmpty()) {
            finish(order);
        } else {
            player.sendSystemMessage(Component.translatable("ae_compendium.order.planning", order.jobs.size())
                .withStyle(ChatFormatting.GRAY));
            PENDING.add(order);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Order> it = PENDING.iterator(); it.hasNext(); ) {
            Order order = it.next();
            if (order.player.isRemoved()) {
                order.jobs.forEach(job -> job.plan().cancel(true));
                it.remove();
                continue;
            }
            for (Iterator<Job> jobs = order.jobs.iterator(); jobs.hasNext(); ) {
                Job job = jobs.next();
                if (!job.plan().isDone()) continue;
                jobs.remove();
                ICraftingPlan plan;
                try {
                    plan = job.plan().get();
                } catch (Exception e) {
                    order.lines.add(line("failed", ChatFormatting.RED, job.key(), job.amount()));
                    continue;
                }
                if (!plan.missingItems().isEmpty()) {
                    order.lines.add(line("missing_resources", ChatFormatting.YELLOW, job.key(), job.amount()));
                    continue;
                }
                var result = order.grid.getCraftingService().submitJob(plan, null, null, false, order.source);
                if (result.successful()) {
                    order.submitted++;
                    order.lines.add(line("submitted", ChatFormatting.GREEN, job.key(), job.amount()));
                } else {
                    order.lines.add(line("no_cpu", ChatFormatting.YELLOW, job.key(), job.amount()));
                }
            }
            if (order.jobs.isEmpty()) {
                finish(order);
                it.remove();
            }
        }
    }

    private static void finish(Order order) {
        order.player.sendSystemMessage(Component.translatable("ae_compendium.order.header", order.submitted)
            .withStyle(ChatFormatting.AQUA));
        order.lines.forEach(order.player::sendSystemMessage);
    }

    private static Component line(String kind, ChatFormatting color, AEItemKey key, long amount) {
        return Component.translatable("ae_compendium.order." + kind, key.getDisplayName(), amount).withStyle(color);
    }

    /** The grid of the AE wireless access point the held gadget is bound to (Building Gadgets' AE2 link). */
    private static @Nullable IGrid boundGrid(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BaseGadget)) continue;
            GlobalPos bound = GadgetNBT.getBoundPos(stack);
            if (bound == null) continue;
            ServerLevel level = player.level().getServer().getLevel(bound.dimension());
            if (level == null || !level.isLoaded(bound.pos())) continue;
            if (level.getBlockEntity(bound.pos()) instanceof IWirelessAccessPoint accessPoint) {
                IGrid grid = accessPoint.getGrid();
                if (grid != null) return grid;
            }
        }
        return null;
    }
}
