package dev.upiscium.frontierprotocol.cleanup;

import com.Harbinger.Spore.core.Sblocks;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.SporeGameTestAssertions;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionManager;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionSavedData;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServerInfectionCleanupGameTests {
    private static final ServerInfectionCleanupService SERVICE = ServerInfectionCleanupService.INSTANCE;
    private static final ServerInfectionCleanupService.BlockMutator NORMAL_MUTATOR = ServerLevel::setBlock;
    private static final CleanupSourceProfile DEFAULT_PROFILE = new CleanupSourceProfile(20, 10, 10);

    private ServerInfectionCleanupGameTests() {}

    @GameTest(template = "empty", batch = "cleanup_service_safety", timeoutTicks = 200)
    public static void loadedChunkMutationAndSafetyPolicy(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();
        ChunkPos chunk = new ChunkPos(-40, -40);
        level.getChunk(chunk.x, chunk.z);
        long chunkKey = chunk.toLong();
        BlockPos removablePos = position(level, chunk, 0);
        BlockPos neverPos = position(level, chunk, 1);
        BlockPos blockEntityPos = position(level, chunk, 2);
        SuppressionSourceId source = source("safety");

        try {
            SERVICE.clearRuntime(server);
            placeRemovable(level, removablePos);
            level.setBlock(neverPos, Sblocks.INFESTED_STONE.get().defaultBlockState(), Block.UPDATE_NONE);
            level.setBlock(blockEntityPos, Sblocks.CONTAINER.get().defaultBlockState(), Block.UPDATE_NONE);
            helper.assertTrue(
                    level.getBlockState(removablePos).is(Sblocks.GROWTHS_BIG.get()),
                    "removable setup block was not retained");
            helper.assertTrue(level.getBlockEntity(blockEntityPos) != null, "container Block Entity was not created");
            setCursor(level, chunkKey, 0);
            SERVICE.registerActiveSource(
                    level, source, Set.of(chunk), CleanupActivationMode.RESUME, DEFAULT_PROFILE);

            ServerInfectionCleanupService.CleanupTickResult result = SERVICE.tick(
                    server, settings(3, 3), NORMAL_MUTATOR);

            helper.assertTrue(result.inspected() == 3, "cleanup did not inspect the configured three positions");
            helper.assertTrue(
                    result.mutated() == 1,
                    "cleanup mutation count was " + result.mutated() + " instead of 1");
            helper.assertTrue(level.getBlockState(removablePos).isAir(), "removable Spore foliage was not replaced");
            helper.assertTrue(
                    level.getBlockState(neverPos).is(Sblocks.INFESTED_STONE.get()),
                    "cleanup/never block was replaced");
            helper.assertTrue(
                    level.getBlockState(blockEntityPos).is(Sblocks.CONTAINER.get()),
                    "Block Entity state was replaced");
            helper.assertTrue(level.getBlockEntity(blockEntityPos) != null, "Block Entity was removed");
        } finally {
            SERVICE.clearRuntime(server);
            clear(level, removablePos, removablePos.below(), neverPos, blockEntityPos);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cleanup_service_budgets", timeoutTicks = 200)
    public static void budgetsAndFailedMutationPreserveCursor(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();

        try {
            assertMutationDefers(helper, level, server, new ChunkPos(-41, -40), source("zero_mutation"));
            assertFailedMutationDefers(helper, level, server, new ChunkPos(-42, -40), source("failed_mutation"));
            assertGlobalBudgets(helper, level, server, new ChunkPos(-43, -40), source("global_budget"));
            assertSourceCycleBudgets(helper, level, server, new ChunkPos(-44, -40), source("source_budget"));
        } finally {
            SERVICE.clearRuntime(server);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cleanup_service_resume", timeoutTicks = 200)
    public static void unloadedChunkAndRestartResumeKeepCursor(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();
        ChunkPos unloaded = new ChunkPos(-1_000_000, -1_000_000);
        long unloadedKey = unloaded.toLong();

        try {
            SERVICE.clearRuntime(server);
            helper.assertTrue(
                    level.getChunkSource().getChunkNow(unloaded.x, unloaded.z) == null,
                    "unloaded test chunk was already loaded");
            setCursor(level, unloadedKey, 0);
            SERVICE.registerActiveSource(
                    level,
                    source("unloaded"),
                    Set.of(unloaded),
                    CleanupActivationMode.RESUME,
                    DEFAULT_PROFILE);
            ServerInfectionCleanupService.CleanupTickResult unloadedResult =
                    SERVICE.tick(server, settings(8, 8), NORMAL_MUTATOR);
            helper.assertTrue(unloadedResult.inspected() == 0, "unloaded chunk was inspected");
            helper.assertTrue(
                    level.getChunkSource().getChunkNow(unloaded.x, unloaded.z) == null,
                    "cleanup forced the unloaded chunk to load");
            assertCursor(helper, level, unloadedKey, 0, "unloaded cursor advanced");

            assertRuntimeResume(helper, level, server);
        } finally {
            SERVICE.clearRuntime(server);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cleanup_service_dimensions", timeoutTicks = 200)
    public static void dimensionsAlternateAndNegativeChunkMutates(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();
        ServerLevel nether = server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether level is unavailable");
        ChunkPos overworldChunk = new ChunkPos(-50, -50);
        ChunkPos netherChunk = new ChunkPos(-51, -50);
        BlockPos negativeTarget = position(overworld, overworldChunk, 0);

        try {
            SERVICE.clearRuntime(server);
            overworld.getChunk(overworldChunk.x, overworldChunk.z);
            nether.getChunk(netherChunk.x, netherChunk.z);
            setCursor(overworld, overworldChunk.toLong(), 0);
            setCursor(nether, netherChunk.toLong(), 0);
            SERVICE.registerActiveSource(
                    overworld,
                    source("dimension_overworld"),
                    Set.of(overworldChunk),
                    CleanupActivationMode.RESUME,
                    DEFAULT_PROFILE);
            SERVICE.registerActiveSource(
                    nether,
                    source("dimension_nether"),
                    Set.of(netherChunk),
                    CleanupActivationMode.RESUME,
                    DEFAULT_PROFILE);

            ServerInfectionCleanupService.CleanupTickResult fairness =
                    SERVICE.tick(server, settings(2, 0), NORMAL_MUTATOR);
            helper.assertTrue(fairness.inspected() == 2, "dimension fairness test missed an inspection");
            helper.assertTrue(
                    fairness.inspectedDimensions().size() == 2
                            && fairness.inspectedDimensions().get(0) != fairness.inspectedDimensions().get(1),
                    "scheduler did not alternate dimensions");

            SERVICE.clearRuntime(server);
            placeRemovable(overworld, negativeTarget);
            setCursor(overworld, overworldChunk.toLong(), 0);
            SERVICE.registerActiveSource(
                    overworld,
                    source("negative_chunk"),
                    Set.of(overworldChunk),
                    CleanupActivationMode.RESUME,
                    DEFAULT_PROFILE);
            ServerInfectionCleanupService.CleanupTickResult negative =
                    SERVICE.tick(server, settings(1, 1), NORMAL_MUTATOR);
            helper.assertTrue(negative.mutated() == 1, "negative chunk cleanup did not mutate once");
            helper.assertTrue(overworld.getBlockState(negativeTarget).isAir(), "negative chunk target remains");
        } finally {
            SERVICE.clearRuntime(server);
            clear(overworld, negativeTarget, negativeTarget.below());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cleanup_service_activation", timeoutTicks = 200)
    public static void activationTransitionsApplyThroughService(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();

        try {
            SERVICE.clearRuntime(server);
            assertOverlappingNewPassThroughService(helper, level, server);
            SERVICE.clearRuntime(server);
            assertDuplicateRegistrationThroughService(helper, level);
            SERVICE.clearRuntime(server);
            assertActiveExpansionThroughService(helper, level);
            SERVICE.clearRuntime(server);
            assertShrinkOverlapThroughService(helper, level);
            SERVICE.clearRuntime(server);
            assertPausedCoverageMoveThroughService(helper, level);
        } finally {
            SERVICE.clearRuntime(server);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cleanup_service_profiles", timeoutTicks = 200)
    public static void distinctSourceBudgetsRemainUnderGlobalCap(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();
        ChunkPos limitedChunk = new ChunkPos(-74, -70);
        ChunkPos largerChunk = new ChunkPos(-75, -70);

        try {
            SERVICE.clearRuntime(server);
            level.getChunk(limitedChunk.x, limitedChunk.z);
            level.getChunk(largerChunk.x, largerChunk.z);
            setCursor(level, limitedChunk.toLong(), 0);
            setCursor(level, largerChunk.toLong(), 0);
            SERVICE.registerActiveSource(
                    level,
                    source("profile_limited"),
                    Set.of(limitedChunk),
                    CleanupActivationMode.RESUME,
                    new CleanupSourceProfile(20, 1, 1));
            SERVICE.registerActiveSource(
                    level,
                    source("profile_larger"),
                    Set.of(largerChunk),
                    CleanupActivationMode.RESUME,
                    new CleanupSourceProfile(20, 3, 1));

            ServerInfectionCleanupService.CleanupTickResult first =
                    SERVICE.tick(server, settings(2, 0), NORMAL_MUTATOR);
            ServerInfectionCleanupService.CleanupTickResult second =
                    SERVICE.tick(server, settings(2, 0), NORMAL_MUTATOR);

            helper.assertTrue(first.inspected() == 2, "distinct profiles did not reach the global cap");
            helper.assertTrue(second.inspected() == 2, "larger source profile did not retain its independent budget");
            assertCursor(helper, level, limitedChunk.toLong(), 1, "limited source exceeded its inspection budget");
            assertCursor(helper, level, largerChunk.toLong(), 3, "larger source budget was not enforced exactly");
        } finally {
            SERVICE.clearRuntime(server);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "initial_spawn_no_cleanup", timeoutTicks = 200)
    public static void initialSpawnSuppressionDoesNotCreateCleanupWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();
        boolean originalEnabled = FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.get();
        int originalRadius = FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.get();
        boolean originalCleanupEnabled = FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.get();
        Map<Long, CleanupProgress> cleanupBefore = InfectionCleanupSavedData.get(level).snapshot();
        BlockPos[] placed = new BlockPos[1];

        try {
            FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(true);
            FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(2);
            FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(true);
            ServerInfectionSuppressionService.INSTANCE.clear(level);
            SpawnProtectionManager.rebuild(level);
            SERVICE.clearRuntime(server);

            ChunkPos spawnChunk = SpawnProtectionSavedData.get(level).centerChunk();
            level.getChunk(spawnChunk.x, spawnChunk.z);
            BlockPos foliage = new BlockPos(spawnChunk.getMinBlockX() + 2, 64, spawnChunk.getMinBlockZ() + 2);
            placed[0] = foliage;
            placeRemovable(level, foliage);
            helper.assertTrue(
                    ServerInfectionSuppressionService.INSTANCE.isSuppressed(level, spawnChunk),
                    "initial spawn suppression is not active");
            SporeGameTestAssertions.assertProtoMutationBlocked(
                    helper,
                    level,
                    foliage.offset(2, 0, 0),
                    "initial spawn source did not suppress a new Spore mutation");
            helper.assertTrue(SERVICE.activeTaskCount(level) == 0, "initial spawn source created a cleanup task");
            helper.runAfterDelay(20, () -> verifyInitialSpawnDoesNotClean(
                    helper,
                    level,
                    spawnChunk,
                    foliage,
                    cleanupBefore,
                    originalEnabled,
                    originalRadius,
                    originalCleanupEnabled));
        } catch (RuntimeException error) {
            restoreInitialSpawnTest(
                    level,
                    placed[0],
                    cleanupBefore,
                    originalEnabled,
                    originalRadius,
                    originalCleanupEnabled);
            throw error;
        }
    }

    private static void verifyInitialSpawnDoesNotClean(
            GameTestHelper helper,
            ServerLevel level,
            ChunkPos spawnChunk,
            BlockPos foliage,
            Map<Long, CleanupProgress> cleanupBefore,
            boolean originalEnabled,
            int originalRadius,
            boolean originalCleanupEnabled) {
        try {
            helper.assertTrue(
                    level.getBlockState(foliage).is(Sblocks.GROWTHS_BIG.get()),
                    "initial spawn suppression cleaned existing removable foliage");
            helper.assertTrue(
                    InfectionCleanupSavedData.get(level).snapshot().equals(cleanupBefore),
                    "initial spawn source created or advanced cleanup progress for " + spawnChunk);
            helper.assertTrue(SERVICE.activeTaskCount(level) == 0, "initial spawn source queued cleanup work");
            restoreInitialSpawnTest(
                    level, foliage, cleanupBefore, originalEnabled, originalRadius, originalCleanupEnabled);
            helper.succeed();
        } catch (RuntimeException error) {
            restoreInitialSpawnTest(
                    level, foliage, cleanupBefore, originalEnabled, originalRadius, originalCleanupEnabled);
            throw error;
        }
    }

    private static void restoreInitialSpawnTest(
            ServerLevel level,
            BlockPos foliage,
            Map<Long, CleanupProgress> cleanupBefore,
            boolean originalEnabled,
            int originalRadius,
            boolean originalCleanupEnabled) {
        if (foliage != null) clear(level, foliage, foliage.below(), foliage.offset(2, 0, 0));
        SERVICE.clearRuntime(level.getServer());
        InfectionCleanupSavedData.get(level).restoreSnapshot(cleanupBefore);
        FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(originalEnabled);
        FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(originalRadius);
        FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.set(originalCleanupEnabled);
        ServerInfectionSuppressionService.INSTANCE.clear(level);
        SpawnProtectionManager.rebuild(level);
    }

    private static void assertOverlappingNewPassThroughService(
            GameTestHelper helper, ServerLevel level, MinecraftServer server) {
        ChunkPos chunk = new ChunkPos(-70, -70);
        long chunkKey = chunk.toLong();
        level.getChunk(chunk.x, chunk.z);
        SERVICE.registerActiveSource(
                level, source("overlap_a"), Set.of(chunk), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);
        InfectionCleanupSavedData.get(level)
                .update(
                        chunkKey,
                        new CleanupProgress(
                                new CleanupCursor(level.getSectionsCount() - 1, 4095, true),
                                false,
                                level.getMinSection(),
                                level.getSectionsCount()));
        SERVICE.tick(server, settings(1, 1), NORMAL_MUTATOR);
        helper.assertTrue(SERVICE.activeTaskCount(level) == 0, "completed overlap task remained queued");

        SERVICE.registerActiveSource(
                level, source("overlap_b"), Set.of(chunk), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);

        helper.assertTrue(
                InfectionCleanupSavedData.get(level).snapshot().get(chunkKey).cursor().equals(CleanupCursor.start()),
                "overlapping NEW_PASS did not reset completed cursor");
        helper.assertTrue(SERVICE.activeTaskCount(level) == 1, "overlapping NEW_PASS did not requeue one task");
    }

    private static void assertDuplicateRegistrationThroughService(
            GameTestHelper helper, ServerLevel level) {
        ChunkPos chunk = new ChunkPos(-71, -70);
        long chunkKey = chunk.toLong();
        SuppressionSourceId source = source("duplicate_registration");
        SERVICE.registerActiveSource(
                level, source, Set.of(chunk), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);
        CleanupProgress partial = new CleanupProgress(
                new CleanupCursor(cleanupSectionIndex(level), 321, false),
                false,
                level.getMinSection(),
                level.getSectionsCount());
        InfectionCleanupSavedData.get(level).update(chunkKey, partial);

        SERVICE.registerActiveSource(
                level, source, Set.of(chunk), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);

        helper.assertTrue(
                InfectionCleanupSavedData.get(level).snapshot().get(chunkKey).equals(partial),
                "duplicate active NEW_PASS reset cursor");
    }

    private static void assertActiveExpansionThroughService(GameTestHelper helper, ServerLevel level) {
        ChunkPos retained = new ChunkPos(-76, -70);
        ChunkPos added = new ChunkPos(-77, -70);
        long retainedKey = retained.toLong();
        long addedKey = added.toLong();
        SuppressionSourceId source = source("active_expansion");
        SERVICE.registerActiveSource(
                level, source, Set.of(retained), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);
        setCursor(level, retainedKey, 31);
        setCursor(level, addedKey, 47);

        SERVICE.registerActiveSource(
                level,
                source,
                Set.of(retained, added),
                CleanupActivationMode.NEW_PASS,
                DEFAULT_PROFILE);

        assertCursor(helper, level, retainedKey, 31, "ACTIVE expansion reset retained coverage");
        helper.assertTrue(
                InfectionCleanupSavedData.get(level)
                        .snapshot()
                        .get(addedKey)
                        .cursor()
                        .equals(CleanupCursor.start()),
                "ACTIVE expansion did not start new coverage at NEW_PASS");
    }

    private static void assertShrinkOverlapThroughService(GameTestHelper helper, ServerLevel level) {
        ChunkPos activeChunk = new ChunkPos(-78, -70);
        ChunkPos pausedChunk = new ChunkPos(-79, -70);
        ChunkPos finalChunk = new ChunkPos(-80, -70);
        SuppressionSourceId shrinking = source("shrinking");
        SuppressionSourceId activeOverlap = source("shrink_active_overlap");
        SuppressionSourceId pausedOverlap = source("shrink_paused_overlap");
        SERVICE.registerActiveSource(
                level,
                shrinking,
                Set.of(activeChunk, pausedChunk, finalChunk),
                CleanupActivationMode.NEW_PASS,
                DEFAULT_PROFILE);
        SERVICE.registerActiveSource(
                level, activeOverlap, Set.of(activeChunk), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);
        SERVICE.registerPausedSource(
                level, pausedOverlap, Set.of(pausedChunk), CleanupActivationMode.RESUME, DEFAULT_PROFILE);
        setCursor(level, activeChunk.toLong(), 12);
        setCursor(level, pausedChunk.toLong(), 13);
        setCursor(level, finalChunk.toLong(), 14);

        SERVICE.registerActiveSource(
                level, shrinking, Set.of(), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);

        Map<Long, CleanupProgress> progress = InfectionCleanupSavedData.get(level).snapshot();
        helper.assertTrue(!progress.get(activeChunk.toLong()).restartRequired(), "active overlap was restarted on shrink");
        helper.assertTrue(!progress.get(pausedChunk.toLong()).restartRequired(), "paused overlap was restarted on shrink");
        helper.assertTrue(progress.get(finalChunk.toLong()).restartRequired(), "final removed coverage was not restarted");
    }

    private static void assertPausedCoverageMoveThroughService(
            GameTestHelper helper, ServerLevel level) {
        ChunkPos previous = new ChunkPos(-72, -70);
        ChunkPos next = new ChunkPos(-73, -70);
        long previousKey = previous.toLong();
        long nextKey = next.toLong();
        SuppressionSourceId source = source("paused_move");
        SERVICE.registerActiveSource(
                level, source, Set.of(previous), CleanupActivationMode.NEW_PASS, DEFAULT_PROFILE);
        SERVICE.pauseSource(level, source);
        setCursor(level, previousKey, 11);
        setCursor(level, nextKey, 22);

        SERVICE.registerActiveSource(
                level, source, Set.of(next), CleanupActivationMode.RESUME, DEFAULT_PROFILE);

        CleanupProgress oldProgress =
                InfectionCleanupSavedData.get(level).snapshot().get(previousKey);
        CleanupProgress nextProgress = InfectionCleanupSavedData.get(level).snapshot().get(nextKey);
        helper.assertTrue(oldProgress.restartRequired(), "paused removed coverage was not marked for restart");
        helper.assertTrue(
                nextProgress.cursor().localBlockIndex() == 22,
                "paused source RESUME did not preserve next coverage cursor");
    }

    private static void assertMutationDefers(
            GameTestHelper helper,
            ServerLevel level,
            MinecraftServer server,
            ChunkPos chunk,
            SuppressionSourceId source) {
        SERVICE.clearRuntime(server);
        level.getChunk(chunk.x, chunk.z);
        BlockPos first = position(level, chunk, 0);
        BlockPos target = position(level, chunk, 1);
        placeRemovable(level, first);
        placeRemovable(level, target);
        setCursor(level, chunk.toLong(), 0);
        SERVICE.registerActiveSource(
                level,
                source,
                Set.of(chunk),
                CleanupActivationMode.RESUME,
                new CleanupSourceProfile(20, 2, 1));

        ServerInfectionCleanupService.CleanupTickResult firstResult =
                SERVICE.tick(server, settings(1, 1), NORMAL_MUTATOR);
        helper.assertTrue(firstResult.mutated() == 1, "source mutation setup did not consume its budget");

        ServerInfectionCleanupService.CleanupTickResult result =
                SERVICE.tick(server, settings(1, 1), NORMAL_MUTATOR);
        helper.assertTrue(result.inspected() == 1 && result.mutated() == 0, "zero source mutation budget was exceeded");
        assertCursor(helper, level, chunk.toLong(), 1, "cursor advanced without source mutation budget");
        helper.assertTrue(level.getBlockState(target).is(Sblocks.GROWTHS_BIG.get()), "deferred target was replaced");
        clear(level, first, first.below(), target, target.below());
    }

    private static void assertFailedMutationDefers(
            GameTestHelper helper,
            ServerLevel level,
            MinecraftServer server,
            ChunkPos chunk,
            SuppressionSourceId source) {
        SERVICE.clearRuntime(server);
        level.getChunk(chunk.x, chunk.z);
        BlockPos target = position(level, chunk, 0);
        placeRemovable(level, target);
        setCursor(level, chunk.toLong(), 0);
        SERVICE.registerActiveSource(
                level,
                source,
                Set.of(chunk),
                CleanupActivationMode.RESUME,
                new CleanupSourceProfile(20, 1, 1));

        ServerInfectionCleanupService.CleanupTickResult result =
                SERVICE.tick(server, settings(1, 1), (ignoredLevel, ignoredPos, ignoredState, ignoredFlags) -> false);
        helper.assertTrue(result.inspected() == 1 && result.mutated() == 0, "failed setBlock counted as mutation");
        assertCursor(helper, level, chunk.toLong(), 0, "cursor advanced after failed setBlock");
        clear(level, target, target.below());
    }

    private static void assertGlobalBudgets(
            GameTestHelper helper,
            ServerLevel level,
            MinecraftServer server,
            ChunkPos chunk,
            SuppressionSourceId source) {
        SERVICE.clearRuntime(server);
        level.getChunk(chunk.x, chunk.z);
        for (int index = 0; index < 3; index++) {
            level.setBlock(position(level, chunk, index), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
        }
        setCursor(level, chunk.toLong(), 0);
        SERVICE.registerActiveSource(
                level, source, Set.of(chunk), CleanupActivationMode.RESUME, DEFAULT_PROFILE);
        ServerInfectionCleanupService.CleanupTickResult inspections =
                SERVICE.tick(server, settings(2, 2), NORMAL_MUTATOR);
        helper.assertTrue(inspections.inspected() == 2, "global inspection budget was not enforced exactly");

        SERVICE.clearRuntime(server);
        for (int index = 0; index < 3; index++) {
            placeRemovable(level, position(level, chunk, index));
        }
        setCursor(level, chunk.toLong(), 0);
        SERVICE.registerActiveSource(
                level, source, Set.of(chunk), CleanupActivationMode.RESUME, DEFAULT_PROFILE);

        ServerInfectionCleanupService.CleanupTickResult result =
                SERVICE.tick(server, settings(3, 1), NORMAL_MUTATOR);
        helper.assertTrue(result.inspected() <= 3, "global mutation test exceeded inspection budget");
        helper.assertTrue(result.mutated() == 1, "global mutation budget was exceeded");
        for (int index = 0; index < 3; index++) {
            BlockPos pos = position(level, chunk, index);
            clear(level, pos, pos.below());
        }
    }

    private static void assertSourceCycleBudgets(
            GameTestHelper helper,
            ServerLevel level,
            MinecraftServer server,
            ChunkPos chunk,
            SuppressionSourceId source) {
        SERVICE.clearRuntime(server);
        level.getChunk(chunk.x, chunk.z);
        for (int index = 0; index < 3; index++) {
            placeRemovable(level, position(level, chunk, index));
        }
        setCursor(level, chunk.toLong(), 0);
        SERVICE.registerActiveSource(
                level,
                source,
                Set.of(chunk),
                CleanupActivationMode.RESUME,
                new CleanupSourceProfile(20, 2, 1));

        ServerInfectionCleanupService.CleanupTickResult result =
                SERVICE.tick(server, settings(10, 10), NORMAL_MUTATOR);
        helper.assertTrue(result.inspected() == 2, "source inspection cycle budget was not enforced exactly");
        helper.assertTrue(
                result.mutated() == 1,
                "source mutation count was " + result.mutated() + " instead of 1");
        for (int index = 0; index < 3; index++) {
            BlockPos pos = position(level, chunk, index);
            clear(level, pos, pos.below());
        }
    }

    private static void assertRuntimeResume(
            GameTestHelper helper, ServerLevel level, MinecraftServer server) {
        ChunkPos chunk = new ChunkPos(-60, -61);
        long chunkKey = chunk.toLong();
        SuppressionSourceId source = source("runtime_resume");
        level.getChunk(chunk.x, chunk.z);
        setCursor(level, chunkKey, 100);
        SERVICE.registerActiveSource(
                level, source, Set.of(chunk), CleanupActivationMode.RESUME, DEFAULT_PROFILE);
        SERVICE.tick(server, settings(1, 0), NORMAL_MUTATOR);
        assertCursor(helper, level, chunkKey, 101, "first runtime cursor did not advance");

        SERVICE.clearRuntime(server);
        SERVICE.registerActiveSource(
                level, source, Set.of(chunk), CleanupActivationMode.RESUME, DEFAULT_PROFILE);
        assertCursor(helper, level, chunkKey, 101, "clearRuntime discarded persisted cursor");
        SERVICE.tick(server, settings(1, 0), NORMAL_MUTATOR);
        assertCursor(helper, level, chunkKey, 102, "RESUME did not continue persisted cursor");
    }

    private static ServerInfectionCleanupService.CleanupSettings settings(
            int globalInspections, int globalMutations) {
        return new ServerInfectionCleanupService.CleanupSettings(true, globalInspections, globalMutations);
    }

    private static void setCursor(ServerLevel level, long chunkKey, int localIndex) {
        InfectionCleanupSavedData.get(level)
                .update(
                        chunkKey,
                        new CleanupProgress(
                                new CleanupCursor(cleanupSectionIndex(level), localIndex, false),
                                false,
                                level.getMinSection(),
                                level.getSectionsCount()));
    }

    private static void assertCursor(
            GameTestHelper helper, ServerLevel level, long chunkKey, int expectedIndex, String message) {
        CleanupCursor cursor = InfectionCleanupSavedData.get(level)
                .snapshot()
                .get(chunkKey)
                .cursor();
        helper.assertTrue(
                cursor.sectionIndex() == cleanupSectionIndex(level)
                        && cursor.localBlockIndex() == expectedIndex,
                message);
    }

    private static BlockPos position(ServerLevel level, ChunkPos chunk, int localIndex) {
        return new CleanupCursor(cleanupSectionIndex(level), localIndex, false)
                .blockPos(chunk, level.getMinSection());
    }

    private static SuppressionSourceId source(String path) {
        return new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath(
                FrontierProtocolMod.MOD_ID, "cleanup_service_" + path));
    }

    private static void clear(ServerLevel level, BlockPos... positions) {
        for (BlockPos pos : positions) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
    }

    private static void placeRemovable(ServerLevel level, BlockPos pos) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
        level.setBlock(pos, Sblocks.GROWTHS_BIG.get().defaultBlockState(), Block.UPDATE_NONE);
    }

    private static int cleanupSectionIndex(ServerLevel level) {
        return level.getSectionIndex(64);
    }
}
