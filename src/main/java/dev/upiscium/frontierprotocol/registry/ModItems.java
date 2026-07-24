package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrontierProtocolMod.MOD_ID);
    public static final DeferredItem<BlockItem> TIER_1_STABILIZER = ITEMS.registerSimpleBlockItem(
            "tier_1_stabilizer", ModBlocks.TIER_1_STABILIZER);
    public static final DeferredItem<BlockItem> TIER_2_STABILIZER = ITEMS.registerSimpleBlockItem(
            "tier_2_stabilizer", ModBlocks.TIER_2_STABILIZER);
    public static final DeferredItem<Item> STABILIZATION_COMPOUND = ITEMS.registerSimpleItem(
            "stabilization_compound", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> STABILIZATION_CELL = ITEMS.registerSimpleItem(
            "stabilization_cell", new Item.Properties().stacksTo(64));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
