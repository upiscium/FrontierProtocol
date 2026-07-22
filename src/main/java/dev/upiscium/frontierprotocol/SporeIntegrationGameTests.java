package dev.upiscium.frontierprotocol;

import com.Harbinger.Spore.Sentities.FoliageSpread;
import com.Harbinger.Spore.core.Sblocks;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SporeIntegrationGameTests {
    private static final ChunkPos PROTECTED_CHUNK = new ChunkPos(-120, -120);
    private static final ChunkPos OUTSIDE_CHUNK = new ChunkPos(-118, -120);
    private static final SuppressionSource SOURCE = new SuppressionSource(
            new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath(
                    FrontierProtocolMod.MOD_ID, "spore_integration_gametest")),
            SuppressionSourceType.EXTERNAL);

    private SporeIntegrationGameTests() {}

    @GameTest(template = "empty", batch = "spore_integration", timeoutTicks = 100)
    public static void foliageSpreadQueriesSuppressionForSourcesAndTargets(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerInfectionSuppressionService service = ServerInfectionSuppressionService.INSTANCE;
        BlockPos protectedPos = center(PROTECTED_CHUNK);
        BlockPos outsidePos = center(OUTSIDE_CHUNK);
        TrackingSpread spread = new TrackingSpread();

        try {
            service.registerOrUpdateSource(overworld, SOURCE, Set.of(PROTECTED_CHUNK));
            helper.assertTrue(service.isSuppressed(overworld, protectedPos), "test target is not suppressed");
            helper.assertFalse(service.isSuppressed(overworld, outsidePos), "outside target is unexpectedly suppressed");

            spread.SpreadInfection(overworld, 0.0, protectedPos);
            helper.assertTrue(spread.additionCalls == 0, "protected spread source ran preliminary placers");
            spread.SpreadInfection(overworld, 0.0, outsidePos);
            helper.assertTrue(spread.additionCalls == 1, "outside spread source was cancelled");

            BlockState wheat = Blocks.WHEAT.defaultBlockState();
            overworld.setBlock(protectedPos, wheat, Block.UPDATE_NONE);
            repeatTargetSpread(spread, overworld, wheat, protectedPos, 1024);
            helper.assertTrue(overworld.getBlockState(protectedPos).equals(wheat),
                    "protected Spore target was mutated");

            BlockState existingInfection = Sblocks.ROTTEN_CROPS.get().defaultBlockState();
            overworld.setBlock(protectedPos, existingInfection, Block.UPDATE_NONE);
            repeatTargetSpread(spread, overworld, existingInfection, protectedPos, 32);
            helper.assertTrue(overworld.getBlockState(protectedPos).equals(existingInfection),
                    "suppression removed existing Spore infection");

            overworld.setBlock(outsidePos, wheat, Block.UPDATE_NONE);
            repeatUntilChanged(spread, overworld, wheat, outsidePos, 4096);
            helper.assertFalse(overworld.getBlockState(outsidePos).equals(wheat),
                    "outside Spore target did not execute its normal conversion path");
        } finally {
            service.unregisterSource(overworld, SOURCE.id());
            overworld.setBlock(protectedPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
            overworld.setBlock(outsidePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
        helper.succeed();
    }

    private static void repeatTargetSpread(
            FoliageSpread spread, Level level, BlockState state, BlockPos target, int attempts) {
        for (int i = 0; i < attempts; i++) {
            spread.SpreadFoliageAndConvert(level, state, target);
        }
    }

    private static void repeatUntilChanged(
            FoliageSpread spread, Level level, BlockState state, BlockPos target, int attempts) {
        for (int i = 0; i < attempts && level.getBlockState(target).equals(state); i++) {
            spread.SpreadFoliageAndConvert(level, state, target);
        }
    }

    private static BlockPos center(ChunkPos chunk) {
        return new BlockPos(chunk.getMinBlockX() + 8, 64, chunk.getMinBlockZ() + 8);
    }

    private static final class TrackingSpread implements FoliageSpread {
        private int additionCalls;

        @Override
        public void additionPlacers(Level level, BlockPos pos, double range) {
            additionCalls++;
        }

        @Override
        public void additionIgnoreConfigPlacers(Level level, BlockPos pos, double range) {
            additionCalls++;
        }
    }
}
