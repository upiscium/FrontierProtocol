package dev.upiscium.frontierprotocol.cleanup;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.compat.spore.SporeCleanupPolicy;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class ServerInfectionCleanupService {
    public static final ServerInfectionCleanupService INSTANCE = new ServerInfectionCleanupService();
    private static final int DEBUG_INTERVAL_TICKS = 200;
    private static final int CLEANUP_UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

    private final Map<ServerLevel, DimensionCleanupIndex> indexes = new IdentityHashMap<>();
    private final Map<MinecraftServer, RoundRobinCleanupQueue<ServerLevel>> dimensionsByServer =
            new IdentityHashMap<>();
    private final Map<MinecraftServer, DebugCounters> debugCounters = new IdentityHashMap<>();

    private ServerInfectionCleanupService() {}

    public void registerActiveSource(
            ServerLevel level,
            SuppressionSourceId sourceId,
            Set<ChunkPos> coveredChunks,
            CleanupActivationMode activationMode,
            CleanupSourceProfile profile) {
        requireServerThread(level);
        DimensionCleanupIndex index = indexes.computeIfAbsent(level, ignored -> new DimensionCleanupIndex());
        DimensionCleanupIndex.ActivationChanges changes =
                index.registerActive(sourceId, coveredChunks, activationMode, profile);
        applyActivationChanges(level, index, changes);
    }

    public void registerPausedSource(
            ServerLevel level,
            SuppressionSourceId sourceId,
            Set<ChunkPos> coveredChunks,
            CleanupActivationMode activationMode,
            CleanupSourceProfile profile) {
        requireServerThread(level);
        DimensionCleanupIndex index = indexes.computeIfAbsent(level, ignored -> new DimensionCleanupIndex());
        DimensionCleanupIndex.ActivationChanges changes =
                index.registerPaused(sourceId, coveredChunks, activationMode, profile);
        applyActivationChanges(level, index, changes);
    }

    private void applyActivationChanges(
            ServerLevel level,
            DimensionCleanupIndex index,
            DimensionCleanupIndex.ActivationChanges changes) {
        InfectionCleanupSavedData data = InfectionCleanupSavedData.get(level);

        for (long chunkKey : changes.noLongerRegistered()) {
            data.markRestartRequired(chunkKey, level.getMinSection(), level.getSectionsCount());
        }
        for (long chunkKey : changes.newPassChunks()) {
            data.activate(
                    chunkKey,
                    level.getMinSection(),
                    level.getSectionsCount(),
                    CleanupActivationMode.NEW_PASS);
            index.resumeIncomplete(chunkKey);
        }
        for (long chunkKey : changes.globallyNewlyActive()) {
            if (changes.newPassChunks().contains(chunkKey)) continue;
            CleanupProgress progress = data.activate(
                    chunkKey,
                    level.getMinSection(),
                    level.getSectionsCount(),
                    CleanupActivationMode.RESUME);
            if (progress.cursor().completed()) {
                index.suspendCompleted(chunkKey);
            } else {
                index.resumeIncomplete(chunkKey);
            }
        }
        syncDimensionQueue(level, index);
    }

    public void pauseSource(ServerLevel level, SuppressionSourceId sourceId) {
        requireServerThread(level);
        DimensionCleanupIndex index = indexes.get(level);
        if (index == null) return;
        index.pause(sourceId);
        syncDimensionQueue(level, index);
    }

    public void deactivateSource(ServerLevel level, SuppressionSourceId sourceId) {
        requireServerThread(level);
        DimensionCleanupIndex index = indexes.get(level);
        if (index == null) return;
        InfectionCleanupSavedData data = InfectionCleanupSavedData.get(level);
        for (long chunkKey : index.deactivate(sourceId)) {
            data.markRestartRequired(chunkKey, level.getMinSection(), level.getSectionsCount());
        }
        syncDimensionQueue(level, index);
    }

    public void tick(MinecraftServer server) {
        runTick(server, CleanupSettings.fromConfig(), ServerLevel::setBlock, false);
    }

    CleanupTickResult tick(
            MinecraftServer server, CleanupSettings settings, BlockMutator blockMutator) {
        return runTick(server, settings, blockMutator, true);
    }

    private CleanupTickResult runTick(
            MinecraftServer server,
            CleanupSettings settings,
            BlockMutator blockMutator,
            boolean collectInspectedDimensions) {
        requireServerThread(server);
        long started = System.nanoTime();
        DebugCounters counters = debugCounters.computeIfAbsent(server, ignored -> new DebugCounters());
        if (!settings.enabled()) {
            logIfDue(server, counters, null, System.nanoTime() - started);
            return CleanupTickResult.empty();
        }

        for (Map.Entry<ServerLevel, DimensionCleanupIndex> entry : indexes.entrySet()) {
            if (entry.getKey().getServer() == server) {
                entry.getValue().refreshSourceBudgets(server.getTickCount());
            }
        }

        CleanupBudget global =
                new CleanupBudget(settings.globalInspectionBudget(), settings.globalMutationBudget());
        int activeTasks = activeTaskCount(server);
        Set<TaskIdentity> attemptedWithoutProgress = new HashSet<>();
        Map<ServerLevel, Set<Long>> blockedTasks = new IdentityHashMap<>();
        RoundRobinCleanupQueue<ServerLevel> dimensions = dimensionsByServer.get(server);
        List<ResourceKey<Level>> inspectedDimensions = collectInspectedDimensions ? new ArrayList<>() : List.of();

        while (global.canInspect()
                && activeTasks > 0
                && attemptedWithoutProgress.size() < activeTasks) {
            if (dimensions == null) break;
            ServerLevel level = dimensions.next();
            if (level == null) break;
            DimensionCleanupIndex index = indexes.get(level);
            if (index == null || index.activeTaskCount() == 0) {
                dimensions.remove(level);
                continue;
            }

            Long chunkKey = index.nextTask();
            if (chunkKey == null) {
                dimensions.remove(level);
                continue;
            }
            TaskIdentity task = new TaskIdentity(level, chunkKey);
            if (blockedTasks.getOrDefault(level, Set.of()).contains(chunkKey)) {
                attemptedWithoutProgress.add(task);
                continue;
            }
            DimensionCleanupIndex.SourceRegistration sponsor = index.sponsor(chunkKey, global.canMutate());
            if (sponsor == null) {
                attemptedWithoutProgress.add(task);
                continue;
            }

            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey));
            if (chunk == null) {
                counters.unloadedSkippedTasks++;
                attemptedWithoutProgress.add(task);
                continue;
            }
            counters.loadedTaskVisits++;

            ProcessResult result = inspectOne(level, chunk, chunkKey, index, sponsor, global, blockMutator);
            counters.inspectedBlocks += result.inspected();
            counters.mutatedBlocks += result.mutated();
            if (collectInspectedDimensions && result.inspected() > 0) {
                inspectedDimensions.add(level.dimension());
            }
            if (result.completed()) {
                counters.completedTasks++;
                index.suspendCompleted(chunkKey);
                syncDimensionQueue(level, index);
            }
            if (result.blocked()) {
                blockedTasks
                        .computeIfAbsent(level, ignored -> new HashSet<>())
                        .add(chunkKey);
            }
            if (result.advanced() || (result.completed() && result.inspected() == 0)) {
                attemptedWithoutProgress.clear();
            } else {
                attemptedWithoutProgress.add(task);
            }
            activeTasks = activeTaskCount(server);
        }

        logIfDue(server, counters, global, System.nanoTime() - started);
        return new CleanupTickResult(
                settings.globalInspectionBudget() - global.inspectionsRemaining(),
                settings.globalMutationBudget() - global.mutationsRemaining(),
                List.copyOf(inspectedDimensions));
    }

    public void clearRuntime(ServerLevel level) {
        requireServerThread(level);
        indexes.remove(level);
        RoundRobinCleanupQueue<ServerLevel> dimensions = dimensionsByServer.get(level.getServer());
        if (dimensions != null) {
            dimensions.remove(level);
            if (dimensions.size() == 0) dimensionsByServer.remove(level.getServer());
        }
    }

    public void clearRuntime(MinecraftServer server) {
        requireServerThread(server);
        indexes.keySet().removeIf(level -> level.getServer() == server);
        dimensionsByServer.remove(server);
        debugCounters.remove(server);
    }

    int activeTaskCount(ServerLevel level) {
        DimensionCleanupIndex index = indexes.get(level);
        return index == null ? 0 : index.activeTaskCount();
    }

    private ProcessResult inspectOne(
            ServerLevel level,
            LevelChunk chunk,
            long chunkKey,
            DimensionCleanupIndex index,
            DimensionCleanupIndex.SourceRegistration sponsor,
            CleanupBudget global,
            BlockMutator blockMutator) {
        InfectionCleanupSavedData data = InfectionCleanupSavedData.get(level);
        CleanupProgress progress = data.progress(chunkKey, level.getMinSection(), level.getSectionsCount());
        CleanupCursor cursor = progress.cursor();
        if (cursor.completed()) return new ProcessResult(0, 0, true, false, false);

        LevelChunkSection section = chunk.getSections()[cursor.sectionIndex()];
        BlockState state = section.getBlockState(cursor.localX(), cursor.localY(), cursor.localZ());
        global.consumeInspection();
        sponsor.budget().consumeInspection();

        Optional<BlockState> proposedReplacement = SporeCleanupPolicy.replacementFor(state);
        if (proposedReplacement.isEmpty()) {
            return advance(data, chunkKey, progress, false, false);
        }
        if (!global.canMutate() || !sponsor.budget().canMutate()) {
            return new ProcessResult(1, 0, false, true, false);
        }
        if (!index.hasActiveSources(chunkKey)
                || level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) != chunk) {
            return new ProcessResult(1, 0, false, true, false);
        }

        BlockPos pos = cursor.blockPos(chunk.getPos(), level.getMinSection());
        BlockState currentState = chunk.getBlockState(pos);
        if (currentState.hasBlockEntity()
                || chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) != null) {
            return advance(data, chunkKey, progress, false, false);
        }
        Optional<BlockState> replacement = SporeCleanupPolicy.replacementFor(currentState);
        if (replacement.isEmpty()) return advance(data, chunkKey, progress, false, false);
        if (!global.canMutate() || !sponsor.budget().canMutate()) {
            return new ProcessResult(1, 0, false, true, false);
        }
        if (!blockMutator.setBlock(level, pos, replacement.orElseThrow(), CLEANUP_UPDATE_FLAGS)) {
            return new ProcessResult(1, 0, false, true, false);
        }

        global.consumeMutation();
        sponsor.budget().consumeMutation();
        ProcessResult advanced = advance(data, chunkKey, progress, true, true);
        return new ProcessResult(1, 1, advanced.completed(), false, true);
    }

    private static ProcessResult advance(
            InfectionCleanupSavedData data,
            long chunkKey,
            CleanupProgress progress,
            boolean cleanupCandidate,
            boolean mutationSucceeded) {
        CleanupCursor nextCursor = progress.cursor().afterInspection(
                progress.sectionCount(), cleanupCandidate, mutationSucceeded);
        data.update(chunkKey, progress.withCursor(nextCursor));
        return new ProcessResult(1, 0, nextCursor.completed(), false, true);
    }

    private void syncDimensionQueue(ServerLevel level, DimensionCleanupIndex index) {
        RoundRobinCleanupQueue<ServerLevel> dimensions = dimensionsByServer.computeIfAbsent(
                level.getServer(), ignored -> new RoundRobinCleanupQueue<>());
        if (index.activeTaskCount() > 0) {
            dimensions.add(level);
        } else {
            dimensions.remove(level);
        }
        if (index.isEmpty()) indexes.remove(level);
        if (dimensions.size() == 0) dimensionsByServer.remove(level.getServer());
    }

    private int activeTaskCount(MinecraftServer server) {
        int count = 0;
        for (Map.Entry<ServerLevel, DimensionCleanupIndex> entry : indexes.entrySet()) {
            if (entry.getKey().getServer() == server) count += entry.getValue().activeTaskCount();
        }
        return count;
    }

    private void logIfDue(
            MinecraftServer server, DebugCounters counters, CleanupBudget global, long elapsedNanoseconds) {
        counters.elapsedNanoseconds += elapsedNanoseconds;
        if (Math.floorMod(server.getTickCount(), DEBUG_INTERVAL_TICKS) != 0) return;

        int activeSources = 0;
        int activeTasks = 0;
        int completedActiveTasks = 0;
        for (Map.Entry<ServerLevel, DimensionCleanupIndex> entry : indexes.entrySet()) {
            if (entry.getKey().getServer() != server) continue;
            DimensionCleanupIndex index = entry.getValue();
            activeSources += index.activeSourceCount();
            activeTasks += index.activeTaskCount();
            completedActiveTasks += index.completedActiveTaskCount();
        }
        if (FrontierProtocolServerConfig.DEBUG_LOGGING.get()) {
            FrontierProtocolMod.LOGGER.debug(
                    "Cleanup summary: activeSources={}, activeTasks={}, loadedTaskVisits={}, unloadedSkippedTasks={}, "
                            + "completedActiveTasks={}, completedThisInterval={}, inspectedBlocks={}, mutatedBlocks={}, "
                            + "remainingInspectionBudget={}, remainingMutationBudget={}, elapsedNanoseconds={}",
                    activeSources,
                    activeTasks,
                    counters.loadedTaskVisits,
                    counters.unloadedSkippedTasks,
                    completedActiveTasks,
                    counters.completedTasks,
                    counters.inspectedBlocks,
                    counters.mutatedBlocks,
                    global == null ? 0 : global.inspectionsRemaining(),
                    global == null ? 0 : global.mutationsRemaining(),
                    counters.elapsedNanoseconds);
        }
        counters.reset();
    }

    private static void requireServerThread(ServerLevel level) {
        requireServerThread(level.getServer());
    }

    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Cleanup service must be accessed on the server thread");
        }
    }

    private record TaskIdentity(ServerLevel level, long chunkKey) {}

    @FunctionalInterface
    interface BlockMutator {
        boolean setBlock(ServerLevel level, BlockPos pos, BlockState state, int flags);
    }

    record CleanupSettings(
            boolean enabled, int globalInspectionBudget, int globalMutationBudget) {
        CleanupSettings {
            if (globalInspectionBudget < 0 || globalMutationBudget < 0) {
                throw new IllegalArgumentException("Cleanup settings contain an invalid budget or interval");
            }
        }

        static CleanupSettings fromConfig() {
            return new CleanupSettings(
                    FrontierProtocolServerConfig.PROGRESSIVE_CLEANUP_ENABLED.get(),
                    FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.get(),
                    FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.get());
        }
    }

    record CleanupTickResult(
            int inspected, int mutated, List<ResourceKey<Level>> inspectedDimensions) {
        private static CleanupTickResult empty() {
            return new CleanupTickResult(0, 0, List.of());
        }
    }

    private record ProcessResult(
            int inspected, int mutated, boolean completed, boolean blocked, boolean advanced) {}

    private static final class DebugCounters {
        private long loadedTaskVisits;
        private long unloadedSkippedTasks;
        private long completedTasks;
        private long inspectedBlocks;
        private long mutatedBlocks;
        private long elapsedNanoseconds;

        private void reset() {
            loadedTaskVisits = 0;
            unloadedSkippedTasks = 0;
            completedTasks = 0;
            inspectedBlocks = 0;
            mutatedBlocks = 0;
            elapsedNanoseconds = 0;
        }
    }
}
