package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.BlockItem;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrontierProtocolMod.MOD_ID);
    public static final DeferredItem<BlockItem> STABILIZATION_BEACON = ITEMS.registerSimpleBlockItem(ModBlocks.STABILIZATION_BEACON);
    public static final DeferredItem<BlockItem> INFECTION_CORE = ITEMS.registerSimpleBlockItem(ModBlocks.INFECTION_CORE);
    public static final DeferredItem<BlockItem> INFECTION_NEST = ITEMS.registerSimpleBlockItem(ModBlocks.INFECTION_NEST);
    public static final DeferredItem<BlockItem> RESOURCE_NODE = ITEMS.registerSimpleBlockItem(ModBlocks.RESOURCE_NODE);
    public static final DeferredItem<BlockItem> OIL_WELL = ITEMS.registerSimpleBlockItem(ModBlocks.OIL_WELL);

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
