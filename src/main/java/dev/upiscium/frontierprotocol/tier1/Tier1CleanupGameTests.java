package dev.upiscium.frontierprotocol.tier1;

import com.Harbinger.Spore.core.Sblocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.cleanup.CleanupCursor;
import dev.upiscium.frontierprotocol.cleanup.CleanupProgress;
import dev.upiscium.frontierprotocol.cleanup.InfectionCleanupSavedData;
import dev.upiscium.frontierprotocol.cleanup.ServerInfectionCleanupService;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Tier1CleanupGameTests {
    private Tier1CleanupGameTests() {}

    @GameTest(template = "empty", batch = "tier1_cleanup_lifecycle", timeoutTicks = 200)
    public static void tierOneCleanupFollowsLifecycleAndReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos(origin);
        BlockPos device = new BlockPos(chunk.getMinBlockX() + 8, origin.getY() + 1, chunk.getMinBlockZ() + 8);
        BlockPos target = new BlockPos(chunk.getMinBlockX() + 2, 64, chunk.getMinBlockZ() + 2);
        TestContext context = new TestContext(helper, level, device, null, target, ConfigSnapshot.capture());

        runStage(context, () -> {
            configureCleanupTest();
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
            placeDevice(level, device);
            insertCell(level, device, 2, helper);
            placeRemovable(level, target);
            setCursor(level, chunk, target);
            helper.runAfterDelay(2, () -> verifyOfflineDoesNotClean(context));
        });
    }

    @GameTest(template = "empty", batch = "tier1_cleanup_overlap", timeoutTicks = 200)
    public static void overlappingTierOneCleanupKeepsOneSharedTask(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos(origin);
        BlockPos first = new BlockPos(chunk.getMinBlockX() + 5, origin.getY() + 1, chunk.getMinBlockZ() + 8);
        BlockPos second = new BlockPos(chunk.getMinBlockX() + 11, origin.getY() + 1, chunk.getMinBlockZ() + 8);
        BlockPos target = new BlockPos(chunk.getMinBlockX() + 2, 64, chunk.getMinBlockZ() + 2);
        TestContext context = new TestContext(helper, level, first, second, target, ConfigSnapshot.capture());

        runStage(context, () -> {
            configureCleanupTest();
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(20);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.set(512);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(128);
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
            placeDevice(level, first);
            placeDevice(level, second);
            insertCell(level, first, 2, helper);
            insertCell(level, second, 2, helper);
            placeMotor(level, first.west());
            placeMotor(level, second.west());
            helper.runAfterDelay(12, () -> verifyOverlappingCleanup(context));
        });
    }

    @GameTest(template = "empty", batch = "tier1_cleanup_grace_reload", timeoutTicks = 200)
    public static void graceReloadRestoresPausedCleanupRegistration(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos(origin);
        BlockPos device = new BlockPos(chunk.getMinBlockX() + 8, origin.getY() + 1, chunk.getMinBlockZ() + 8);
        BlockPos target = new BlockPos(chunk.getMinBlockX() + 2, 64, chunk.getMinBlockZ() + 2);
        TestContext context = new TestContext(helper, level, device, null, target, ConfigSnapshot.capture());

        runStage(context, () -> {
            configureCleanupTest();
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(20);
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
            placeDevice(level, device);
            insertCell(level, device, 2, helper);
            placeMotor(level, device.west());
            helper.runAfterDelay(10, () -> enterGraceForReload(context));
        });
    }

    @GameTest(template = "empty", batch = "tier1_cleanup_grace_overlap_reload", timeoutTicks = 200)
    public static void graceOverlapReloadKeepsPausedCoverageRegistered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos(origin);
        BlockPos first = new BlockPos(chunk.getMinBlockX() + 5, origin.getY() + 1, chunk.getMinBlockZ() + 8);
        BlockPos second = new BlockPos(chunk.getMinBlockX() + 11, origin.getY() + 1, chunk.getMinBlockZ() + 8);
        BlockPos target = new BlockPos(chunk.getMinBlockX() + 2, 64, chunk.getMinBlockZ() + 2);
        TestContext context = new TestContext(helper, level, first, second, target, ConfigSnapshot.capture());

        runStage(context, () -> {
            configureCleanupTest();
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(20);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.set(512);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(128);
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
            placeDevice(level, first);
            placeDevice(level, second);
            insertCell(level, first, 2, helper);
            insertCell(level, second, 2, helper);
            placeMotor(level, first.west());
            placeMotor(level, second.west());
            helper.runAfterDelay(10, () -> enterGraceForOverlapReload(context));
        });
    }

    private static void enterGraceForReload(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE,
                    "Tier 1 was not active before GRACE reload test");
            context.level().setBlock(context.first().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(3, () -> reloadGraceWithEmptyRuntime(context));
        });
    }

    private static void reloadGraceWithEmptyRuntime(TestContext context) {
        runStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.level(), context.first());
            context.helper().assertTrue(
                    blockEntity.status() == Tier1StabilizerStatus.GRACE_PERIOD,
                    "Tier 1 did not enter GRACE before runtime clear");
            placeRemovable(context.level(), context.target());
            CleanupProgress partial = setCursor(
                    context.level(), new ChunkPos(context.first()), context.target());
            CompoundTag saved = blockEntity.saveWithFullMetadata(context.level().registryAccess());

            ServerInfectionCleanupService.INSTANCE.clearRuntime(context.level().getServer());
            reloadBlockEntity(context.level(), context.first(), saved);
            context.helper().runAfterDelay(3, () -> verifyGraceReloadPaused(context, partial));
        });
    }

    private static void verifyGraceReloadPaused(TestContext context, CleanupProgress partial) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.GRACE_PERIOD,
                    "reloaded GRACE Tier 1 changed status");
            context.helper().assertTrue(
                    ServerInfectionSuppressionService.INSTANCE.isSuppressed(
                            context.level(), new ChunkPos(context.first())),
                    "reloaded GRACE Tier 1 lost suppression");
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).is(Sblocks.GROWTHS_BIG.get()),
                    "reloaded GRACE Tier 1 performed cleanup");
            context.helper().assertTrue(
                    progress(context).equals(partial), "reloaded GRACE Tier 1 changed its cleanup cursor");
            placeMotor(context.level(), context.first().west());
            context.helper().runAfterDelay(5, () -> verifyGraceReloadResumed(context));
        });
    }

    private static void verifyGraceReloadResumed(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE,
                    "reloaded GRACE Tier 1 did not return to ACTIVE");
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).isAir(),
                    "reloaded GRACE Tier 1 did not RESUME from the partial cursor");
            cleanup(context);
            context.helper().succeed();
        });
    }

    private static void enterGraceForOverlapReload(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE
                            && blockEntity(context.level(), context.second()).status() == Tier1StabilizerStatus.ACTIVE,
                    "overlap reload devices were not active");
            context.level().setBlock(context.first().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(3, () -> rebuildGraceOverlapRuntime(context));
        });
    }

    private static void rebuildGraceOverlapRuntime(TestContext context) {
        runStage(context, () -> {
            Tier1StabilizerBlockEntity first = blockEntity(context.level(), context.first());
            Tier1StabilizerBlockEntity second = blockEntity(context.level(), context.second());
            context.helper().assertTrue(
                    first.status() == Tier1StabilizerStatus.GRACE_PERIOD
                            && second.status() == Tier1StabilizerStatus.ACTIVE,
                    "overlap reload did not start with A=GRACE and B=ACTIVE");
            CleanupProgress partial = setCursor(
                    context.level(), new ChunkPos(context.first()), context.target());
            CompoundTag firstSaved = first.saveWithFullMetadata(context.level().registryAccess());
            CompoundTag secondSaved = second.saveWithFullMetadata(context.level().registryAccess());

            FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(false);
            ServerInfectionCleanupService.INSTANCE.clearRuntime(context.level().getServer());
            reloadBlockEntity(context.level(), context.first(), firstSaved);
            reloadBlockEntity(context.level(), context.second(), secondSaved);
            context.helper().runAfterDelay(2, () -> removeActiveOverlapAfterReload(context, partial));
        });
    }

    private static void removeActiveOverlapAfterReload(TestContext context, CleanupProgress partial) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.GRACE_PERIOD
                            && blockEntity(context.level(), context.second()).status()
                                    == Tier1StabilizerStatus.ACTIVE,
                    "reloaded overlap did not restore A=GRACE and B=ACTIVE");

            context.level().destroyBlock(context.second(), false);
            context.helper().assertTrue(
                    progress(context).equals(partial),
                    "destroying active overlap reset the shared cursor or marked restartRequired");
            placeRemovable(context.level(), context.target());
            FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(true);
            context.helper().runAfterDelay(2, () -> verifyReloadedPausedOverlap(context, partial));
        });
    }

    private static void verifyReloadedPausedOverlap(TestContext context, CleanupProgress partial) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).is(Sblocks.GROWTHS_BIG.get()),
                    "reloaded paused overlap continued cleanup after active source removal");
            context.helper().assertTrue(
                    progress(context).equals(partial),
                    "reloaded paused overlap changed its shared cursor");
            context.helper().assertTrue(
                    !progress(context).restartRequired(),
                    "paused overlap registration did not prevent restartRequired");
            placeMotor(context.level(), context.first().west());
            context.helper().runAfterDelay(10, () -> verifyReloadedOverlapResume(context));
        });
    }

    private static void verifyReloadedOverlapResume(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE,
                    "reloaded paused overlap did not return to ACTIVE");
            CleanupProgress current = progress(context);
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).isAir(),
                    "reloaded paused overlap did not RESUME its shared cursor: cursor="
                            + current.cursor() + ", restartRequired=" + current.restartRequired());
            cleanup(context);
            context.helper().succeed();
        });
    }

    private static void verifyOfflineDoesNotClean(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.OFFLINE,
                    "offline Tier 1 unexpectedly became active");
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).is(Sblocks.GROWTHS_BIG.get()),
                    "offline Tier 1 cleaned existing infection");
            Tier1StabilizerBlockEntity oldBlockEntity = blockEntity(context.level(), context.first());
            CompoundTag saved = oldBlockEntity.saveWithFullMetadata(context.level().registryAccess());
            BlockState state = context.level().getBlockState(context.first());
            context.level().removeBlockEntity(context.first());
            Tier1StabilizerBlockEntity reloaded = new Tier1StabilizerBlockEntity(context.first(), state);
            reloaded.loadWithComponents(saved, context.level().registryAccess());
            context.level().setBlockEntity(reloaded);
            setCompleted(context.level(), new ChunkPos(context.first()));
            placeMotor(context.level(), context.first().west());
            context.helper().runAfterDelay(12, () -> verifyFreshActiveCleanup(context));
        });
    }

    private static void verifyFreshActiveCleanup(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE,
                    "powered Tier 1 did not become active");
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).isAir(),
                    "fresh ACTIVE Tier 1 did not execute its NEW_PASS cleanup");
            context.level().setBlock(context.first().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(3, () -> prepareGracePauseCheck(context));
        });
    }

    private static void prepareGracePauseCheck(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.GRACE_PERIOD,
                    "power loss did not pause cleanup in grace period");
            placeRemovable(context.level(), context.target());
            CleanupProgress paused = setCursor(context.level(), new ChunkPos(context.first()), context.target());
            context.helper().runAfterDelay(2, () -> verifyGracePaused(context, paused));
        });
    }

    private static void verifyGracePaused(TestContext context, CleanupProgress paused) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).is(Sblocks.GROWTHS_BIG.get()),
                    "grace-period Tier 1 continued cleanup");
            context.helper().assertTrue(
                    progress(context).equals(paused), "grace-period cleanup advanced its persisted cursor");
            placeMotor(context.level(), context.first().west());
            context.helper().runAfterDelay(5, () -> verifyGraceResume(context));
        });
    }

    private static void verifyGraceResume(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE,
                    "Tier 1 did not recover from grace period");
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).isAir(),
                    "recovered Tier 1 did not RESUME cleanup");

            Tier1StabilizerBlockEntity oldBlockEntity = blockEntity(context.level(), context.first());
            CompoundTag saved = oldBlockEntity.saveWithFullMetadata(context.level().registryAccess());
            oldBlockEntity.onChunkUnloaded();
            BlockPos reloadTarget = context.target().atY(context.level().getMaxBuildHeight() - 2);
            placeRemovable(context.level(), reloadTarget);
            CleanupProgress beforeReload = setCursor(
                    context.level(), new ChunkPos(context.first()), reloadTarget);
            context.helper().assertTrue(
                    !beforeReload.restartRequired(), "chunk unload incorrectly required a new cleanup pass");
            BlockState state = context.level().getBlockState(context.first());
            context.level().removeBlockEntity(context.first());
            Tier1StabilizerBlockEntity reloaded = new Tier1StabilizerBlockEntity(context.first(), state);
            reloaded.loadWithComponents(saved, context.level().registryAccess());
            context.level().setBlockEntity(reloaded);
            context.helper().runAfterDelay(10, () -> verifyReloadResume(context, reloadTarget));
        });
    }

    private static void verifyReloadResume(TestContext context, BlockPos reloadTarget) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE,
                    "reloaded active Tier 1 did not rebuild runtime state");
            context.helper().assertTrue(
                    context.level().getBlockState(reloadTarget).isAir(),
                    "reloaded Tier 1 did not RESUME its saved cleanup cursor");
            context.level().setBlock(reloadTarget.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
            context.level().setBlock(context.first().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(3, () -> prepareOfflineCheck(context));
        });
    }

    private static void prepareOfflineCheck(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.GRACE_PERIOD,
                    "reloaded Tier 1 did not enter grace period");
            placeRemovable(context.level(), context.target());
            setCursor(context.level(), new ChunkPos(context.first()), context.target());
            context.helper().runAfterDelay(12, () -> verifyOfflineDeactivation(context));
        });
    }

    private static void verifyOfflineDeactivation(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.OFFLINE,
                    "expired grace period did not stop Tier 1 cleanup");
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).is(Sblocks.GROWTHS_BIG.get()),
                    "offline Tier 1 continued cleanup");
            context.helper().assertTrue(
                    progress(context).restartRequired(),
                    "final cleanup source deactivation did not require a fresh pass");
            cleanup(context);
            context.helper().succeed();
        });
    }

    private static void verifyOverlappingCleanup(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.ACTIVE
                            && blockEntity(context.level(), context.second()).status() == Tier1StabilizerStatus.ACTIVE,
                    "overlapping Tier 1 devices did not become active");
            context.level().setBlock(context.first().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(3, () -> verifyOnePausedOverlapCleans(context));
        });
    }

    private static void verifyOnePausedOverlapCleans(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.first()).status() == Tier1StabilizerStatus.GRACE_PERIOD,
                    "first overlapping Tier 1 did not enter grace period");
            placeRemovable(context.level(), context.target());
            setCursor(context.level(), new ChunkPos(context.first()), context.target());
            context.helper().runAfterDelay(2, () -> verifyActiveOverlapContinues(context));
        });
    }

    private static void verifyActiveOverlapContinues(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).isAir(),
                    "active overlapping Tier 1 did not continue the shared cleanup task");
            context.level().destroyBlock(context.second(), false);
            placeRemovable(context.level(), context.target());
            CleanupProgress paused = setCursor(context.level(), new ChunkPos(context.first()), context.target());
            context.helper().runAfterDelay(2, () -> verifyFinalPausedOverlapDoesNotClean(context, paused));
        });
    }

    private static void verifyFinalPausedOverlapDoesNotClean(TestContext context, CleanupProgress paused) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).is(Sblocks.GROWTHS_BIG.get()),
                    "paused final overlap source continued cleanup");
            context.helper().assertTrue(
                    progress(context).equals(paused), "paused final overlap source advanced the shared cursor");
            placeMotor(context.level(), context.first().west());
            context.helper().runAfterDelay(5, () -> verifyPausedOverlapResumes(context));
        });
    }

    private static void verifyPausedOverlapResumes(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(
                    context.level().getBlockState(context.target()).isAir(),
                    "remaining Tier 1 did not resume shared cleanup after overlap removal");
            cleanup(context);
            context.helper().succeed();
        });
    }

    private static CleanupProgress setCursor(ServerLevel level, ChunkPos chunk, BlockPos target) {
        int localIndex = (target.getY() & 15) << 8 | (target.getZ() & 15) << 4 | target.getX() & 15;
        CleanupProgress progress = new CleanupProgress(
                new CleanupCursor(level.getSectionIndex(target.getY()), localIndex, false),
                false,
                level.getMinSection(),
                level.getSectionsCount());
        InfectionCleanupSavedData.get(level).update(chunk.toLong(), progress);
        return progress;
    }

    private static void setCompleted(ServerLevel level, ChunkPos chunk) {
        InfectionCleanupSavedData.get(level)
                .update(
                        chunk.toLong(),
                        new CleanupProgress(
                                new CleanupCursor(level.getSectionsCount() - 1, 4095, true),
                                false,
                                level.getMinSection(),
                                level.getSectionsCount()));
    }

    private static CleanupProgress progress(TestContext context) {
        return InfectionCleanupSavedData.get(context.level())
                .snapshot()
                .get(new ChunkPos(context.first()).toLong());
    }

    private static void placeDevice(ServerLevel level, BlockPos pos) {
        level.setBlock(
                pos,
                ModBlocks.TIER_1_STABILIZER
                        .get()
                        .defaultBlockState()
                        .setValue(Tier1StabilizerBlock.HORIZONTAL_AXIS, Direction.Axis.X),
                Block.UPDATE_ALL);
    }

    private static void placeMotor(ServerLevel level, BlockPos pos) {
        level.setBlock(
                pos,
                AllBlocks.CREATIVE_MOTOR
                        .getDefaultState()
                        .setValue(CreativeMotorBlock.FACING, Direction.EAST),
                Block.UPDATE_ALL);
    }

    private static void insertCell(ServerLevel level, BlockPos pos, int count, GameTestHelper helper) {
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(capability != null, "Tier 1 item capability is unavailable");
        ItemStack remainder = capability.insertItem(
                0, new ItemStack(ModItems.STABILIZATION_CELL.get(), count), false);
        helper.assertTrue(remainder.isEmpty(), "Tier 1 rejected stabilization cells");
    }

    private static Tier1StabilizerBlockEntity blockEntity(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Tier1StabilizerBlockEntity blockEntity) return blockEntity;
        throw new IllegalStateException("Tier 1 block entity is missing at " + pos);
    }

    private static Tier1StabilizerBlockEntity reloadBlockEntity(
            ServerLevel level, BlockPos pos, CompoundTag saved) {
        BlockState state = level.getBlockState(pos);
        level.removeBlockEntity(pos);
        Tier1StabilizerBlockEntity reloaded = new Tier1StabilizerBlockEntity(pos, state);
        reloaded.loadWithComponents(saved, level.registryAccess());
        level.setBlockEntity(reloaded);
        return reloaded;
    }

    private static void placeRemovable(ServerLevel level, BlockPos pos) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
        level.setBlock(pos, Sblocks.GROWTHS_BIG.get().defaultBlockState(), Block.UPDATE_NONE);
    }

    private static void configureCleanupTest() {
        FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
        FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(10);
        FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(100);
        FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(true);
        FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.set(8192);
        FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.set(16);
        FrontierProtocolServerConfig.TIER1_CLEANUP_INTERVAL_TICKS.set(1);
        FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(4096);
        FrontierProtocolServerConfig.TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(4);
    }

    private static void runStage(TestContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void cleanup(TestContext context) {
        removeDevice(context.level(), context.first());
        if (context.second() != null) removeDevice(context.level(), context.second());
        context.level().setBlock(context.target(), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        context.level().setBlock(context.target().below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        ServerInfectionCleanupService.INSTANCE.clearRuntime(context.level().getServer());
        context.originalConfig().restore();
    }

    private static void removeDevice(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private record TestContext(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos first,
            BlockPos second,
            BlockPos target,
            ConfigSnapshot originalConfig) {}

    private record ConfigSnapshot(
            int minimumRpm,
            int graceTicks,
            int consumableTicks,
            boolean cleanupEnabled,
            int globalInspections,
            int globalMutations,
            int cleanupInterval,
            int sourceInspections,
            int sourceMutations) {
        private static ConfigSnapshot capture() {
            return new ConfigSnapshot(
                    FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.get(),
                    FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.get(),
                    FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.get(),
                    FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.get(),
                    FrontierProtocolServerConfig.TIER1_CLEANUP_INTERVAL_TICKS.get(),
                    FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.get(),
                    FrontierProtocolServerConfig.TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE.get());
        }

        private void restore() {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(minimumRpm);
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(graceTicks);
            FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(consumableTicks);
            FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(cleanupEnabled);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.set(globalInspections);
            FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.set(globalMutations);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INTERVAL_TICKS.set(cleanupInterval);
            FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(sourceInspections);
            FrontierProtocolServerConfig.TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(sourceMutations);
        }
    }
}
