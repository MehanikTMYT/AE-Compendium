package ru.mehanik.aecompendium;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import ru.mehanik.aecompendium.gadgets.GadgetOrders;
import ru.mehanik.aecompendium.gadgets.OrderMissingPayload;

/**
 * AE Compendium's own code (the other mods in this jar are their original projects).
 *
 * <p>Building Gadgets 2 → AE2 ordering: "Order from AE" in the gadget's material list crafts what is missing through
 * the AE network the gadget is bound to. Written for the pack (owner decision 2026-10-10: GadgetsmeNetwork is AGPL —
 * not used; this is an independent implementation of the idea).
 */
@Mod(AeCompendium.MOD_ID)
public final class AeCompendium {
    public static final String MOD_ID = "ae_compendium";

    public AeCompendium(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
            .playToServer(OrderMissingPayload.TYPE, OrderMissingPayload.STREAM_CODEC, GadgetOrders::handle));
        NeoForge.EVENT_BUS.addListener(GadgetOrders::onServerTick);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
