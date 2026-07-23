package dev.upiscium.frontierprotocol.cleanup;

import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;

final class DimensionCleanupIndex {
    private final Map<SuppressionSourceId, SourceRegistration> registrations = new HashMap<>();
    private final Map<Long, LinkedHashSet<SuppressionSourceId>> activeSourcesByChunk = new HashMap<>();
    private final RoundRobinCleanupQueue<Long> tasks = new RoundRobinCleanupQueue<>();

    public ActivationChanges registerActive(
            SuppressionSourceId sourceId, Set<ChunkPos> coveredChunks, CleanupActivationMode activationMode) {
        SourceRegistration previous = registrations.get(sourceId);
        Set<Long> previousActive = previous != null && previous.active() ? previous.chunkKeys() : Set.of();
        Set<Long> nextKeys = chunkKeys(coveredChunks);
        Set<Long> newlyInactive = new LinkedHashSet<>();
        Set<Long> newlyActive = new LinkedHashSet<>();

        for (long key : previousActive) {
            if (!nextKeys.contains(key)) removeActiveSource(key, sourceId, newlyInactive);
        }
        for (long key : nextKeys) {
            if (!previousActive.contains(key)) addActiveSource(key, sourceId, newlyActive);
        }

        SourceRegistration next = previous == null
                ? new SourceRegistration(sourceId, nextKeys, true)
                : previous.withCoverage(nextKeys, true);
        next.activationMode = activationMode;
        registrations.put(sourceId, next);
        return new ActivationChanges(Set.copyOf(newlyActive), Set.copyOf(newlyInactive));
    }

    public Set<Long> pause(SuppressionSourceId sourceId) {
        SourceRegistration registration = registrations.get(sourceId);
        if (registration == null || !registration.active()) return Set.of();
        Set<Long> newlyInactive = new LinkedHashSet<>();
        for (long key : registration.chunkKeys()) removeActiveSource(key, sourceId, newlyInactive);
        registration.active = false;
        return Set.copyOf(newlyInactive);
    }

    public Set<Long> deactivate(SuppressionSourceId sourceId) {
        SourceRegistration registration = registrations.remove(sourceId);
        if (registration == null) return Set.of();
        Set<Long> ignoredInactive = new LinkedHashSet<>();
        if (registration.active()) {
            for (long key : registration.chunkKeys()) removeActiveSource(key, sourceId, ignoredInactive);
        }
        Set<Long> newlyInactive = new LinkedHashSet<>();
        for (long key : registration.chunkKeys()) {
            if (!hasRegistrationCovering(key)) newlyInactive.add(key);
        }
        return Set.copyOf(newlyInactive);
    }

    public Long nextTask() {
        return tasks.next();
    }

    public void suspendCompleted(long chunkKey) {
        tasks.remove(chunkKey);
    }

    public void resumeIncomplete(long chunkKey) {
        if (hasActiveSources(chunkKey)) tasks.add(chunkKey);
    }

    public boolean hasActiveSources(long chunkKey) {
        Set<SuppressionSourceId> sources = activeSourcesByChunk.get(chunkKey);
        return sources != null && !sources.isEmpty();
    }

    public boolean hasRegistrationCovering(long chunkKey) {
        return hasRegistrationCoveringInternal(chunkKey);
    }

    public int activeTaskCount() {
        return tasks.size();
    }

    public int completedActiveTaskCount() {
        return activeSourcesByChunk.size() - tasks.size();
    }

    public SourceRegistration sponsor(long chunkKey, boolean preferMutation) {
        LinkedHashSet<SuppressionSourceId> sourceIds = activeSourcesByChunk.get(chunkKey);
        if (sourceIds == null) return null;
        SourceRegistration fallback = null;
        SourceRegistration selected = null;
        for (SuppressionSourceId sourceId : sourceIds) {
            SourceRegistration registration = registrations.get(sourceId);
            if (registration == null || !registration.active() || !registration.budget.canInspect()) continue;
            if (!preferMutation || registration.budget.canMutate()) {
                selected = registration;
                break;
            }
            if (fallback == null) fallback = registration;
        }
        if (selected == null) selected = fallback;
        if (selected != null) rotateSponsor(sourceIds, selected.sourceId());
        return selected;
    }

    public void refreshSourceBudgets(
            long gameTick, int intervalTicks, int inspectionBudget, int mutationBudget) {
        for (SourceRegistration registration : registrations.values()) {
            if (registration.active()) {
                registration.refreshBudget(gameTick, intervalTicks, inspectionBudget, mutationBudget);
            }
        }
    }

    public int activeSourceCount() {
        int count = 0;
        for (SourceRegistration registration : registrations.values()) {
            if (registration.active()) count++;
        }
        return count;
    }

    public boolean isEmpty() {
        return registrations.isEmpty();
    }

    private void addActiveSource(long key, SuppressionSourceId sourceId, Set<Long> newlyActive) {
        LinkedHashSet<SuppressionSourceId> sources = activeSourcesByChunk.computeIfAbsent(key, ignored -> {
            newlyActive.add(key);
            tasks.add(key);
            return new LinkedHashSet<>();
        });
        sources.add(sourceId);
    }

    private void removeActiveSource(long key, SuppressionSourceId sourceId, Set<Long> newlyInactive) {
        LinkedHashSet<SuppressionSourceId> sources = activeSourcesByChunk.get(key);
        if (sources == null) return;
        sources.remove(sourceId);
        if (sources.isEmpty()) {
            activeSourcesByChunk.remove(key);
            tasks.remove(key);
            newlyInactive.add(key);
        }
    }

    private static Set<Long> chunkKeys(Set<ChunkPos> chunks) {
        Set<Long> keys = new LinkedHashSet<>(chunks.size());
        for (ChunkPos chunk : chunks) keys.add(chunk.toLong());
        return Set.copyOf(keys);
    }

    private static void rotateSponsor(
            LinkedHashSet<SuppressionSourceId> sourceIds, SuppressionSourceId selectedSource) {
        sourceIds.remove(selectedSource);
        sourceIds.add(selectedSource);
    }

    private boolean hasRegistrationCoveringInternal(long chunkKey) {
        for (SourceRegistration registration : registrations.values()) {
            if (registration.chunkKeys().contains(chunkKey)) return true;
        }
        return false;
    }

    record ActivationChanges(Set<Long> newlyActive, Set<Long> newlyInactive) {}

    static final class SourceRegistration {
        private final SuppressionSourceId sourceId;
        private Set<Long> chunkKeys;
        private boolean active;
        private final CleanupBudget budget = new CleanupBudget(0, 0);
        private long nextCycleTick;
        private boolean cycleInitialized;
        private CleanupActivationMode activationMode;

        private SourceRegistration(SuppressionSourceId sourceId, Set<Long> chunkKeys, boolean active) {
            this.sourceId = sourceId;
            this.chunkKeys = chunkKeys;
            this.active = active;
        }

        private SourceRegistration withCoverage(Set<Long> nextKeys, boolean nextActive) {
            chunkKeys = nextKeys;
            active = nextActive;
            return this;
        }

        private void refreshBudget(long gameTick, int intervalTicks, int inspections, int mutations) {
            if (!cycleInitialized || gameTick >= nextCycleTick) {
                budget.reset(inspections, mutations);
                nextCycleTick = gameTick + intervalTicks;
                cycleInitialized = true;
            }
        }

        public SuppressionSourceId sourceId() {
            return sourceId;
        }

        public Set<Long> chunkKeys() {
            return chunkKeys;
        }

        public boolean active() {
            return active;
        }

        public CleanupBudget budget() {
            return budget;
        }

        public CleanupActivationMode activationMode() {
            return activationMode;
        }
    }
}
