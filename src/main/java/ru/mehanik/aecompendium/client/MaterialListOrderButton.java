package ru.mehanik.aecompendium.client;

import com.direwolf20.buildinggadgets2.client.screen.MaterialListGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.ScrollingMaterialList;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import ru.mehanik.aecompendium.AeCompendium;
import ru.mehanik.aecompendium.gadgets.OrderMissingPayload;

import java.util.ArrayList;
import java.util.List;

/** Adds "Order from AE" to Building Gadgets 2's material list when the held gadget is bound to an AE network. */
@EventBusSubscriber(modid = AeCompendium.MOD_ID, value = Dist.CLIENT)
public final class MaterialListOrderButton {
    private static final int WIDTH = 72;
    private static final int HEIGHT = 14;

    private MaterialListOrderButton() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof MaterialListGUI screen) || !holdsBoundGadget()) {
            return;
        }
        int left = (screen.width - MaterialListGUI.BACKGROUND_WIDTH) / 2;
        int top = (screen.height - MaterialListGUI.BACKGROUND_HEIGHT) / 2;
        Button button = Button.builder(Component.translatable("ae_compendium.order.button"), b -> order(screen))
            .bounds(left + MaterialListGUI.BACKGROUND_WIDTH - MaterialListGUI.BORDER_SIZE - WIDTH - 2,
                top + MaterialListGUI.BORDER_SIZE + 1, WIDTH, HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("ae_compendium.order.button.tooltip")))
            .build();
        event.addListener(button);
    }

    private static boolean holdsBoundGadget() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        for (InteractionHand hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BaseGadget && GadgetNBT.getBoundPos(stack) != null) return true;
        }
        return false;
    }

    private static void order(MaterialListGUI screen) {
        List<OrderMissingPayload.Entry> entries = new ArrayList<>();
        for (var child : screen.children()) {
            if (child instanceof ScrollingMaterialList list) {
                for (ScrollingMaterialList.Entry entry : list.children()) {
                    if (entry.getMissing() > 0 && entries.size() < OrderMissingPayload.MAX_ENTRIES) {
                        entries.add(new OrderMissingPayload.Entry(entry.getStack().copyWithCount(1), entry.getMissing()));
                    }
                }
            }
        }
        var player = Minecraft.getInstance().player;
        if (entries.isEmpty()) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("ae_compendium.order.nothing_missing"));
            }
            return;
        }
        ClientPacketDistributor.sendToServer(new OrderMissingPayload(entries));
        screen.onClose();
    }
}
