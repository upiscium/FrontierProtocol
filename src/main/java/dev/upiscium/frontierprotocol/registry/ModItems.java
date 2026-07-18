package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrontierProtocolMod.MOD_ID);
    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
