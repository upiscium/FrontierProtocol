package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlock;
import dev.upiscium.frontierprotocol.infection.InfectionCoreBlock;
import dev.upiscium.frontierprotocol.infection.InfectionNestBlock;
import dev.upiscium.frontierprotocol.resource.ResourceNodeBlock;
import dev.upiscium.frontierprotocol.oil.OilWellBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FrontierProtocolMod.MOD_ID);
    public static final DeferredBlock<StabilizationBeaconBlock> STABILIZATION_BEACON = BLOCKS.registerBlock(
            "stabilization_beacon",
            StabilizationBeaconBlock::new,
            BlockBehaviour.Properties.of().strength(3.5F, 6.0F).sound(SoundType.METAL));
    public static final DeferredBlock<InfectionCoreBlock> INFECTION_CORE = BLOCKS.registerBlock(
            "infection_core", InfectionCoreBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F, 4.0F).sound(SoundType.SCULK));
    public static final DeferredBlock<InfectionNestBlock> INFECTION_NEST = BLOCKS.registerBlock(
            "infection_nest", InfectionNestBlock::new,
            BlockBehaviour.Properties.of().strength(4.0F, 8.0F).sound(SoundType.SCULK));
    public static final DeferredBlock<ResourceNodeBlock> RESOURCE_NODE = BLOCKS.registerBlock(
            "resource_node", ResourceNodeBlock::new,
            BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL));
    public static final DeferredBlock<OilWellBlock> OIL_WELL = BLOCKS.registerBlock(
            "oil_well", OilWellBlock::new,
            BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
