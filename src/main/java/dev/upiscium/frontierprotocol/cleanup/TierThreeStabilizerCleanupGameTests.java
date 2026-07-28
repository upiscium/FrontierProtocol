package dev.upiscium.frontierprotocol.cleanup;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTierDefinition;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTierDefinitions;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TierThreeStabilizerCleanupGameTests {
    private TierThreeStabilizerCleanupGameTests() {}

    @GameTest(template = "empty", batch = "tier3_cleanup", timeoutTicks = 220)
    public static void tierThreeRegistersTwentyFiveTasksWithItsProfileAndGlobalCap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos center = new ChunkPos(origin);
        BlockPos device = new BlockPos(center.getMinBlockX() + 8, origin.getY() + 1, center.getMinBlockZ() + 8);
        ConfigSnapshot original = ConfigSnapshot.capture();
        Map<Long, CleanupProgress> progressBefore = InfectionCleanupSavedData.get(level).snapshot();
        try {
            FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(false);
            FrontierProtocolServerConfig.TIER3_CHUNK_RADIUS.set(2);
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER3_CLEANUP_INTERVAL_TICKS.set(1200);
            FrontierProtocolServerConfig.TIER3_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(3);
            FrontierProtocolServerConfig.TIER3_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(1);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.set(2);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.set(1);
            for (int x = center.x - 2; x <= center.x + 2; x++) {
                for (int z = center.z - 2; z <= center.z + 2; z++) level.getChunk(x, z);
            }
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
            placeAndPower(level, device, ModBlocks.TIER_3_STABILIZER.get(), helper);
        } catch (RuntimeException error) {
            cleanup(level, original, progressBefore, device);
            throw error;
        }

        helper.runAfterDelay(10, () -> {
            try {
                StabilizerTierDefinition definition = StabilizerTierDefinitions.resolve(StabilizerTier.TIER_3);
                helper.assertTrue(ServerInfectionCleanupService.INSTANCE.activeTaskCount(level) == 25,
                        "active Tier 3 did not register exactly 25 cleanup tasks");
                helper.assertTrue(definition.cleanupProfile().equals(new CleanupSourceProfile(
                                FrontierProtocolServerConfig.TIER3_CLEANUP_INTERVAL_TICKS.get(),
                                FrontierProtocolServerConfig.TIER3_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.get(),
                                FrontierProtocolServerConfig.TIER3_CLEANUP_MUTATION_BUDGET_PER_CYCLE.get())),
                        "Tier 3 cleanup registration does not resolve its configured profile");
                ServerInfectionCleanupService.CleanupTickResult first = tickUsingConfiguredGlobalCap(level);
                ServerInfectionCleanupService.CleanupTickResult second = tickUsingConfiguredGlobalCap(level);
                ServerInfectionCleanupService.CleanupTickResult exhausted = tickUsingConfiguredGlobalCap(level);
                helper.assertTrue(first.inspected() == 2 && second.inspected() == 1 && exhausted.inspected() == 0,
                        "Tier 3 profile/global inspection budgets were not enforced as 2 then 1 then 0");
                helper.assertTrue(first.mutated() == 0 && second.mutated() == 0 && exhausted.mutated() == 0,
                        "Tier 3 cleanup unexpectedly mutated the empty test chunks");
            } finally {
                cleanup(level, original, progressBefore, device);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "tier3_cleanup_mixed_overlap", timeoutTicks = 220)
    public static void mixedTiersShareOneCursorAndTaskWithoutBudgetMultiplication(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        ChunkPos chunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        BlockPos tier1 = new BlockPos(chunk.getMinBlockX() + 3, 65, chunk.getMinBlockZ() + 8);
        BlockPos tier2 = new BlockPos(chunk.getMinBlockX() + 8, 65, chunk.getMinBlockZ() + 8);
        BlockPos tier3 = new BlockPos(chunk.getMinBlockX() + 13, 65, chunk.getMinBlockZ() + 8);
        ConfigSnapshot original = ConfigSnapshot.capture();
        Map<Long, CleanupProgress> progressBefore = InfectionCleanupSavedData.get(level).snapshot();
        MixedContext context = new MixedContext(
                helper, level, chunk, tier1, tier2, tier3, original, progressBefore);
        try {
            FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(false);
            FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.set(0);
            FrontierProtocolServerConfig.TIER2_CHUNK_RADIUS.set(0);
            FrontierProtocolServerConfig.TIER3_CHUNK_RADIUS.set(0);
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INTERVAL_TICKS.set(1200);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.set(1200);
            FrontierProtocolServerConfig.TIER3_CLEANUP_INTERVAL_TICKS.set(1200);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(1);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(2);
            FrontierProtocolServerConfig.TIER3_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(3);
            FrontierProtocolServerConfig.TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(1);
            FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(1);
            FrontierProtocolServerConfig.TIER3_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(1);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.set(2);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.set(1);
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
            placeAndPower(level, tier1, ModBlocks.TIER_1_STABILIZER.get(), helper);
            placeAndPower(level, tier2, ModBlocks.TIER_2_STABILIZER.get(), helper);
            placeAndPower(level, tier3, ModBlocks.TIER_3_STABILIZER.get(), helper);
            helper.runAfterDelay(10, () -> verifySharedTask(context));
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void verifySharedTask(MixedContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(ServerInfectionCleanupService.INSTANCE.activeTaskCount(context.level()) == 1,
                    "three mixed cleanup sources multiplied one overlapping chunk task");
            CleanupProgress starting = new CleanupProgress(
                    new CleanupCursor(0, 123, false),
                    false,
                    context.level().getMinSection(),
                    context.level().getSectionsCount());
            InfectionCleanupSavedData.get(context.level()).update(context.chunk().toLong(), starting);
            ServerInfectionCleanupService.CleanupTickResult first = tickUsingConfiguredGlobalCap(context.level());
            ServerInfectionCleanupService.CleanupTickResult second = tickUsingConfiguredGlobalCap(context.level());
            ServerInfectionCleanupService.CleanupTickResult third = tickUsingConfiguredGlobalCap(context.level());
            ServerInfectionCleanupService.CleanupTickResult exhausted = tickUsingConfiguredGlobalCap(context.level());
            context.helper().assertTrue(first.inspected() == 2
                            && second.inspected() == 2
                            && third.inspected() == 2
                            && exhausted.inspected() == 0,
                    "mixed Tier 1/2/3 budgets were multiplied or did not remain independently available");
            CleanupProgress retained = InfectionCleanupSavedData.get(context.level())
                    .snapshot().get(context.chunk().toLong());
            context.helper().assertTrue(retained.cursor().localBlockIndex() == 129,
                    "mixed cleanup cursor did not advance exactly six globally capped inspections");
            removeDevice(context.level(), context.tier1());
            context.helper().runAfterDelay(2, () -> verifyAfterFirstStop(context, retained));
        });
    }

    private static void verifyAfterFirstStop(MixedContext context, CleanupProgress retained) {
        runStage(context, () -> {
            assertRetained(context, retained, "stopping Tier 1 reset the shared mixed-tier cursor");
            context.helper().assertTrue(ServerInfectionCleanupService.INSTANCE.activeTaskCount(context.level()) == 1,
                    "stopping Tier 1 removed the shared Tier 2/Tier 3 task");
            removeDevice(context.level(), context.tier2());
            context.helper().runAfterDelay(2, () -> verifyAfterSecondStop(context, retained));
        });
    }

    private static void verifyAfterSecondStop(MixedContext context, CleanupProgress retained) {
        runStage(context, () -> {
            assertRetained(context, retained, "stopping Tier 2 reset the shared Tier 3 cursor");
            context.helper().assertTrue(ServerInfectionCleanupService.INSTANCE.activeTaskCount(context.level()) == 1,
                    "stopping Tier 2 removed the remaining Tier 3 task");
            removeDevice(context.level(), context.tier3());
            context.helper().runAfterDelay(2, () -> {
                try {
                    context.helper().assertTrue(
                            ServerInfectionCleanupService.INSTANCE.activeTaskCount(context.level()) == 0,
                            "stopping the last mixed source retained a cleanup task");
                    context.helper().assertTrue(
                            InfectionCleanupSavedData.get(context.level())
                                    .snapshot().get(context.chunk().toLong()).restartRequired(),
                            "stopping the last mixed source did not require a fresh cleanup pass");
                } finally {
                    cleanup(context);
                }
                context.helper().succeed();
            });
        });
    }

    private static void assertRetained(MixedContext context, CleanupProgress retained, String message) {
        context.helper().assertTrue(InfectionCleanupSavedData.get(context.level())
                        .snapshot().get(context.chunk().toLong()).equals(retained), message);
    }

    private static void placeAndPower(
            ServerLevel level, BlockPos position, StabilizerBlock block, GameTestHelper helper) {
        level.setBlock(position, block.defaultBlockState()
                .setValue(StabilizerBlock.HORIZONTAL_AXIS, Direction.Axis.X), Block.UPDATE_ALL);
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, position, Direction.UP);
        helper.assertTrue(capability != null, "mixed cleanup capability is unavailable");
        helper.assertTrue(capability.insertItem(
                        0, new ItemStack(ModItems.STABILIZATION_CELL.get()), false).isEmpty(),
                "mixed cleanup Stabilizer rejected its Cell");
        level.setBlock(position.west(), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
    }

    private static void runStage(MixedContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static ServerInfectionCleanupService.CleanupTickResult tickUsingConfiguredGlobalCap(ServerLevel level) {
        return ServerInfectionCleanupService.INSTANCE.tick(
                level.getServer(),
                new ServerInfectionCleanupService.CleanupSettings(
                        true,
                        FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.get(),
                        FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.get()),
                ServerLevel::setBlock);
    }

    private static void cleanup(
            ServerLevel level,
            ConfigSnapshot original,
            Map<Long, CleanupProgress> progressBefore,
            BlockPos... positions) {
        for (BlockPos position : positions) removeDevice(level, position);
        ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
        InfectionCleanupSavedData.get(level).restoreSnapshot(progressBefore);
        original.restore();
    }

    private static void cleanup(MixedContext context) {
        cleanup(
                context.level(),
                context.original(),
                context.progressBefore(),
                context.tier1(),
                context.tier2(),
                context.tier3());
    }

    private static void removeDevice(ServerLevel level, BlockPos position) {
        level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(position.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private record MixedContext(
            GameTestHelper helper,
            ServerLevel level,
            ChunkPos chunk,
            BlockPos tier1,
            BlockPos tier2,
            BlockPos tier3,
            ConfigSnapshot original,
            Map<Long, CleanupProgress> progressBefore) {}

    private record ConfigSnapshot(
            int tier1Radius,
            int tier2Radius,
            int tier3Radius,
            int tier1Rpm,
            int tier2Rpm,
            int tier3Rpm,
            int tier1Duration,
            int tier2Duration,
            int tier3Duration,
            int tier1CleanupInterval,
            int tier2CleanupInterval,
            int tier3CleanupInterval,
            int tier1CleanupInspections,
            int tier2CleanupInspections,
            int tier3CleanupInspections,
            int tier1CleanupMutations,
            int tier2CleanupMutations,
            int tier3CleanupMutations,
            boolean cleanupEnabled,
            int globalInspections,
            int globalMutations) {
        static ConfigSnapshot capture() {
            return new ConfigSnapshot(
                    FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.get(),
                    FrontierProtocolServerConfig.TIER2_CHUNK_RADIUS.get(),
                    FrontierProtocolServerConfig.TIER3_CHUNK_RADIUS.get(),
                    FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER1_CLEANUP_INTERVAL_TICKS.get(),
                    FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.get(),
                    FrontierProtocolServerConfig.TIER3_CLEANUP_INTERVAL_TICKS.get(),
                    FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.get(),
                    FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.get(),
                    FrontierProtocolServerConfig.TIER3_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.get(),
                    FrontierProtocolServerConfig.TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE.get(),
                    FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.get(),
                    FrontierProtocolServerConfig.TIER3_CLEANUP_MUTATION_BUDGET_PER_CYCLE.get(),
                    FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.get(),
                    FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.get(),
                    FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.get());
        }

        void restore() {
            FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.set(tier1Radius);
            FrontierProtocolServerConfig.TIER2_CHUNK_RADIUS.set(tier2Radius);
            FrontierProtocolServerConfig.TIER3_CHUNK_RADIUS.set(tier3Radius);
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(tier1Rpm);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(tier2Rpm);
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(tier3Rpm);
            FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(tier1Duration);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(tier2Duration);
            FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.set(tier3Duration);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INTERVAL_TICKS.set(tier1CleanupInterval);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.set(tier2CleanupInterval);
            FrontierProtocolServerConfig.TIER3_CLEANUP_INTERVAL_TICKS.set(tier3CleanupInterval);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(tier1CleanupInspections);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(tier2CleanupInspections);
            FrontierProtocolServerConfig.TIER3_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(tier3CleanupInspections);
            FrontierProtocolServerConfig.TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(tier1CleanupMutations);
            FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(tier2CleanupMutations);
            FrontierProtocolServerConfig.TIER3_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(tier3CleanupMutations);
            FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(cleanupEnabled);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.set(globalInspections);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.set(globalMutations);
        }
    }
}
