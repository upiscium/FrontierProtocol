package dev.upiscium.frontierprotocol.registry;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.tier1.Tier1StabilizerBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FrontierProtocolMod.MOD_ID);
    public static final DeferredBlock<Tier1StabilizerBlock> TIER_1_STABILIZER = BLOCKS.registerBlock(
            "tier_1_stabilizer",
            Tier1StabilizerBlock::new,
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops());

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    public static void registerStressImpact() {
        BlockStressValues.IMPACTS.register(
                TIER_1_STABILIZER.get(), FrontierProtocolServerConfig.TIER1_STRESS_IMPACT::getAsDouble);
    }
}
