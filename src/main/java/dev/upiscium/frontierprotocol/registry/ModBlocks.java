package dev.upiscium.frontierprotocol.registry;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTierDefinitions;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FrontierProtocolMod.MOD_ID);
    public static final DeferredBlock<StabilizerBlock> TIER_1_STABILIZER = BLOCKS.registerBlock(
            "tier_1_stabilizer",
            properties -> new StabilizerBlock(StabilizerTier.TIER_1, properties),
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops());

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    public static void registerStressImpact() {
        BlockStressValues.IMPACTS.register(
                TIER_1_STABILIZER.get(),
                () -> StabilizerTierDefinitions.resolve(StabilizerTier.TIER_1).stressImpact());
    }
}
