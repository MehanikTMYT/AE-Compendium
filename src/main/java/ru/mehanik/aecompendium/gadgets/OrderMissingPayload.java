package ru.mehanik.aecompendium.gadgets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import ru.mehanik.aecompendium.AeCompendium;

import java.util.ArrayList;
import java.util.List;

/** Client → server: the materials the player is missing for the held gadget's template (item + amount). */
public record OrderMissingPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 512;

    public record Entry(ItemStack item, int amount) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, Entry::item,
            ByteBufCodecs.VAR_INT, Entry::amount,
            Entry::new);
    }

    public static final Type<OrderMissingPayload> TYPE = new Type<>(AeCompendium.id("order_missing"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrderMissingPayload> STREAM_CODEC = StreamCodec.composite(
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), OrderMissingPayload::entries,
        OrderMissingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
