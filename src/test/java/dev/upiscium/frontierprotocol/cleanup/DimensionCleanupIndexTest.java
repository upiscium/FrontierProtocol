package dev.upiscium.frontierprotocol.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class DimensionCleanupIndexTest {
    private static final ChunkPos OVERLAP = new ChunkPos(-4, 7);
    private static final CleanupSourceProfile PROFILE = new CleanupSourceProfile(20, 4, 2);

    @Test
    void overlapCreatesOneTaskAndOneSourceRemovalKeepsItActive() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        index.registerActive(source("a"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.registerActive(source("b"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);

        assertEquals(1, index.activeTaskCount());
        assertEquals(OVERLAP.toLong(), index.nextTask());

        assertTrue(index.deactivate(source("a")).isEmpty());
        assertTrue(index.hasActiveSources(OVERLAP.toLong()));
        assertEquals(1, index.activeTaskCount());

        assertEquals(Set.of(OVERLAP.toLong()), index.deactivate(source("b")));
        assertFalse(index.hasActiveSources(OVERLAP.toLong()));
        assertEquals(0, index.activeTaskCount());
    }

    @Test
    void pausePreservesRegistrationAndResumeUsesRequestedMode() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId source = source("paused");
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.pause(source);

        assertEquals(0, index.activeTaskCount());
        assertTrue(index.hasRegistrationCovering(OVERLAP.toLong()));
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.RESUME, PROFILE);

        index.refreshSourceBudgets(20);
        DimensionCleanupIndex.SourceRegistration sponsor = index.sponsor(OVERLAP.toLong(), true);
        assertEquals(CleanupActivationMode.RESUME, sponsor.activationMode());
        assertEquals(4, sponsor.budget().inspectionsRemaining());
    }

    @Test
    void deactivatingFinalPausedSourceRequiresRestartButOverlapDoesNot() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId first = source("paused_first");
        SuppressionSourceId second = source("paused_second");
        index.registerActive(first, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.registerActive(second, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.pause(first);
        index.pause(second);

        assertTrue(index.deactivate(first).isEmpty());
        assertEquals(Set.of(OVERLAP.toLong()), index.deactivate(second));
    }

    @Test
    void coverageUpdateRemovesOldTaskAndAddsNewTask() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId source = source("moving");
        ChunkPos next = new ChunkPos(12, -9);
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        DimensionCleanupIndex.ActivationChanges changes =
                index.registerActive(source, Set.of(next), CleanupActivationMode.NEW_PASS, PROFILE);

        assertEquals(Set.of(OVERLAP.toLong()), changes.noLongerRegistered());
        assertEquals(Set.of(next.toLong()), changes.globallyNewlyActive());
        assertEquals(Set.of(next.toLong()), changes.newPassChunks());
        assertEquals(next.toLong(), index.nextTask());
    }

    @Test
    void overlappingNewPassResetsCompletedSharedCursorAndRequeuesSingleTask() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        long chunkKey = OVERLAP.toLong();
        index.registerActive(source("completed_a"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        data.update(
                chunkKey,
                new CleanupProgress(new CleanupCursor(7, 4095, true), false, -4, 8));
        index.suspendCompleted(chunkKey);

        DimensionCleanupIndex.ActivationChanges changes = index.registerActive(
                source("completed_b"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        for (long newPassChunk : changes.newPassChunks()) {
            data.activate(newPassChunk, -4, 8, CleanupActivationMode.NEW_PASS);
            index.resumeIncomplete(newPassChunk);
        }

        assertTrue(changes.globallyNewlyActive().isEmpty());
        assertEquals(Set.of(chunkKey), changes.newPassChunks());
        assertEquals(CleanupCursor.start(), data.snapshot().get(chunkKey).cursor());
        assertEquals(1, data.snapshot().size());
        assertEquals(1, index.activeTaskCount());
        assertEquals(2, index.activeSourceCount(chunkKey));
        assertEquals(chunkKey, index.nextTask());
    }

    @Test
    void duplicateActiveNewPassRegistrationDoesNotResetCursor() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        SuppressionSourceId source = source("duplicate");
        long chunkKey = OVERLAP.toLong();
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        CleanupProgress partial =
                new CleanupProgress(new CleanupCursor(2, 777, false), false, -4, 8);
        data.update(chunkKey, partial);

        DimensionCleanupIndex.ActivationChanges duplicate =
                index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);

        assertTrue(duplicate.globallyNewlyActive().isEmpty());
        assertTrue(duplicate.newPassChunks().isEmpty());
        assertTrue(duplicate.noLongerRegistered().isEmpty());
        assertEquals(partial, data.snapshot().get(chunkKey));
        assertEquals(1, index.activeTaskCount());
    }

    @Test
    void activeExpansionStartsOnlyNewCoverageAndRetainsExistingTask() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId source = source("expanding");
        ChunkPos added = new ChunkPos(OVERLAP.x + 1, OVERLAP.z);
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        long retainedTask = index.nextTask();

        DimensionCleanupIndex.ActivationChanges changes = index.registerActive(
                source, Set.of(OVERLAP, added), CleanupActivationMode.NEW_PASS, PROFILE);

        assertEquals(Set.of(added.toLong()), changes.globallyNewlyActive());
        assertEquals(Set.of(added.toLong()), changes.newPassChunks());
        assertTrue(changes.noLongerRegistered().isEmpty());
        assertEquals(retainedTask, index.nextTask());
    }

    @Test
    void shrinkReportsOnlyCoverageWithNoActiveOrPausedOverlap() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId shrinking = source("shrinking");
        SuppressionSourceId activeOverlap = source("active_shrink_overlap");
        SuppressionSourceId pausedOverlap = source("paused_shrink_overlap");
        ChunkPos activeChunk = new ChunkPos(1, 2);
        ChunkPos pausedChunk = new ChunkPos(2, 2);
        ChunkPos finalChunk = new ChunkPos(3, 2);
        Set<ChunkPos> initial = Set.of(activeChunk, pausedChunk, finalChunk);
        index.registerActive(shrinking, initial, CleanupActivationMode.NEW_PASS, PROFILE);
        index.registerActive(activeOverlap, Set.of(activeChunk), CleanupActivationMode.NEW_PASS, PROFILE);
        index.registerActive(pausedOverlap, Set.of(pausedChunk), CleanupActivationMode.NEW_PASS, PROFILE);
        index.pause(pausedOverlap);

        DimensionCleanupIndex.ActivationChanges changes =
                index.registerActive(shrinking, Set.of(), CleanupActivationMode.NEW_PASS, PROFILE);

        assertEquals(Set.of(finalChunk.toLong()), changes.noLongerRegistered());
        assertTrue(index.hasRegistrationCovering(activeChunk.toLong()));
        assertTrue(index.hasRegistrationCovering(pausedChunk.toLong()));
        assertEquals(Set.of(activeChunk.toLong()), index.deactivate(activeOverlap));
        assertEquals(Set.of(pausedChunk.toLong()), index.deactivate(pausedOverlap));
    }

    @Test
    void pausedRegistrationUpdateStaysPausedAndDoesNotQueueWork() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId source = source("paused_update");
        ChunkPos added = new ChunkPos(9, 9);
        index.registerPaused(source, Set.of(OVERLAP), CleanupActivationMode.RESUME, PROFILE);

        DimensionCleanupIndex.ActivationChanges changes = index.registerPaused(
                source, Set.of(OVERLAP, added), CleanupActivationMode.RESUME, new CleanupSourceProfile(5, 8, 3));

        assertTrue(changes.globallyNewlyActive().isEmpty());
        assertTrue(changes.newPassChunks().isEmpty());
        assertEquals(0, index.activeTaskCount());
        assertTrue(index.hasRegistrationCovering(added.toLong()));
    }

    @Test
    void pausedExpansionStartsOnlyNewCoverageWithoutQueueingWork() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        SuppressionSourceId source = source("paused_expansion");
        ChunkPos added = new ChunkPos(OVERLAP.x + 1, OVERLAP.z);
        long retainedKey = OVERLAP.toLong();
        long addedKey = added.toLong();
        CleanupProgress partial = new CleanupProgress(new CleanupCursor(1, 80, false), false, -4, 8);
        data.update(retainedKey, partial);
        data.update(addedKey, new CleanupProgress(new CleanupCursor(7, 4095, true), false, -4, 8));
        index.registerPaused(source, Set.of(OVERLAP), CleanupActivationMode.RESUME, PROFILE);

        DimensionCleanupIndex.ActivationChanges changes = index.registerPaused(
                source, Set.of(OVERLAP, added), CleanupActivationMode.NEW_PASS, PROFILE);
        for (long newPassChunk : changes.newPassChunks()) {
            data.activate(newPassChunk, -4, 8, CleanupActivationMode.NEW_PASS);
            index.resumeIncomplete(newPassChunk);
        }

        assertEquals(Set.of(addedKey), changes.newPassChunks());
        assertTrue(changes.noLongerRegistered().isEmpty());
        assertEquals(partial, data.snapshot().get(retainedKey));
        assertEquals(CleanupCursor.start(), data.snapshot().get(addedKey).cursor());
        assertEquals(0, index.activeTaskCount());
        assertTrue(index.hasRegistrationCovering(retainedKey));
        assertTrue(index.hasRegistrationCovering(addedKey));
    }

    @Test
    void activeToPausedExpansionStartsOnlyNewCoverageWithoutQueueingWork() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId source = source("active_to_paused_expansion");
        ChunkPos added = new ChunkPos(OVERLAP.x, OVERLAP.z + 1);
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);

        DimensionCleanupIndex.ActivationChanges changes = index.registerPaused(
                source, Set.of(OVERLAP, added), CleanupActivationMode.NEW_PASS, PROFILE);

        assertEquals(Set.of(added.toLong()), changes.newPassChunks());
        assertTrue(changes.noLongerRegistered().isEmpty());
        assertEquals(0, index.activeTaskCount());
        assertTrue(index.hasRegistrationCovering(OVERLAP.toLong()));
        assertTrue(index.hasRegistrationCovering(added.toLong()));
    }

    @Test
    void duplicatePausedNewPassRegistrationDoesNotRestartCoverage() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId source = source("duplicate_paused");
        Set<ChunkPos> coverage = Set.of(OVERLAP, new ChunkPos(OVERLAP.x + 1, OVERLAP.z));
        index.registerPaused(source, coverage, CleanupActivationMode.NEW_PASS, PROFILE);

        DimensionCleanupIndex.ActivationChanges duplicate =
                index.registerPaused(source, coverage, CleanupActivationMode.NEW_PASS, PROFILE);

        assertTrue(duplicate.globallyNewlyActive().isEmpty());
        assertTrue(duplicate.newPassChunks().isEmpty());
        assertTrue(duplicate.noLongerRegistered().isEmpty());
        assertEquals(0, index.activeTaskCount());
    }

    @Test
    void pausedExpansionRetainsOverlappingRegistrationAndOneSharedCursor() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        SuppressionSourceId expanding = source("paused_overlap_expanding");
        SuppressionSourceId overlap = source("paused_overlap_existing");
        ChunkPos added = new ChunkPos(OVERLAP.x + 1, OVERLAP.z);
        long addedKey = added.toLong();
        data.update(addedKey, new CleanupProgress(new CleanupCursor(7, 4095, true), false, -4, 8));
        index.registerPaused(expanding, Set.of(OVERLAP), CleanupActivationMode.RESUME, PROFILE);
        index.registerPaused(overlap, Set.of(added), CleanupActivationMode.RESUME, PROFILE);

        DimensionCleanupIndex.ActivationChanges changes = index.registerPaused(
                expanding, Set.of(OVERLAP, added), CleanupActivationMode.NEW_PASS, PROFILE);
        for (long newPassChunk : changes.newPassChunks()) {
            data.activate(newPassChunk, -4, 8, CleanupActivationMode.NEW_PASS);
        }

        assertEquals(Set.of(addedKey), changes.newPassChunks());
        assertEquals(CleanupCursor.start(), data.snapshot().get(addedKey).cursor());
        assertEquals(1, data.snapshot().size());
        assertEquals(0, index.activeTaskCount());
        assertEquals(Set.of(OVERLAP.toLong()), index.deactivate(expanding));
        assertTrue(index.hasRegistrationCovering(addedKey));
    }

    @Test
    void pausedCoverageChangeReportsUnregisteredChunkAndPreservesResumeCursor() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        SuppressionSourceId source = source("paused_move");
        ChunkPos next = new ChunkPos(5, -11);
        long previousKey = OVERLAP.toLong();
        long nextKey = next.toLong();
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.pause(source);
        data.update(previousKey, new CleanupProgress(new CleanupCursor(1, 80, false), false, -4, 8));
        data.update(nextKey, new CleanupProgress(new CleanupCursor(3, 90, false), false, -4, 8));

        DimensionCleanupIndex.ActivationChanges changes =
                index.registerActive(source, Set.of(next), CleanupActivationMode.RESUME, PROFILE);
        for (long removed : changes.noLongerRegistered()) data.markRestartRequired(removed, -4, 8);
        for (long active : changes.globallyNewlyActive()) {
            data.activate(active, -4, 8, CleanupActivationMode.RESUME);
        }

        assertEquals(Set.of(previousKey), changes.noLongerRegistered());
        assertTrue(data.snapshot().get(previousKey).restartRequired());
        assertEquals(new CleanupCursor(3, 90, false), data.snapshot().get(nextKey).cursor());
    }

    @Test
    void overlapRegistrationPreventsRestartWhenPausedCoverageIsRemoved() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        SuppressionSourceId moving = source("moving_overlap");
        SuppressionSourceId covering = source("paused_covering");
        ChunkPos next = new ChunkPos(3, 4);
        long chunkKey = OVERLAP.toLong();
        index.registerActive(moving, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.registerActive(covering, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.pause(moving);
        index.pause(covering);
        data.update(chunkKey, new CleanupProgress(new CleanupCursor(1, 44, false), false, -4, 8));

        DimensionCleanupIndex.ActivationChanges changes =
                index.registerActive(moving, Set.of(next), CleanupActivationMode.RESUME, PROFILE);
        for (long removed : changes.noLongerRegistered()) data.markRestartRequired(removed, -4, 8);

        assertTrue(changes.noLongerRegistered().isEmpty());
        assertFalse(data.snapshot().get(chunkKey).restartRequired());
    }

    @Test
    void sourceBudgetsRefreshOnlyAtConfiguredCycleBoundary() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        CleanupSourceProfile budgetProfile = new CleanupSourceProfile(20, 2, 1);
        index.registerActive(source("budget"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, budgetProfile);
        index.refreshSourceBudgets(100);
        DimensionCleanupIndex.SourceRegistration sponsor = index.sponsor(OVERLAP.toLong(), true);
        sponsor.budget().consumeInspection();
        sponsor.budget().consumeMutation();

        index.refreshSourceBudgets(119);
        assertEquals(1, sponsor.budget().inspectionsRemaining());
        assertEquals(0, sponsor.budget().mutationsRemaining());
        index.refreshSourceBudgets(120);
        assertEquals(2, sponsor.budget().inspectionsRemaining());
        assertEquals(1, sponsor.budget().mutationsRemaining());
    }

    @Test
    void distinctProfilesRefreshIndependently() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        ChunkPos secondChunk = new ChunkPos(6, 7);
        index.registerActive(
                source("fast"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, new CleanupSourceProfile(5, 2, 1));
        index.registerActive(
                source("slow"), Set.of(secondChunk), CleanupActivationMode.NEW_PASS, new CleanupSourceProfile(20, 7, 3));
        index.refreshSourceBudgets(100);
        DimensionCleanupIndex.SourceRegistration fast = index.sponsor(OVERLAP.toLong(), true);
        DimensionCleanupIndex.SourceRegistration slow = index.sponsor(secondChunk.toLong(), true);
        fast.budget().consumeInspection();
        slow.budget().consumeInspection();

        index.refreshSourceBudgets(105);

        assertEquals(2, fast.budget().inspectionsRemaining());
        assertEquals(6, slow.budget().inspectionsRemaining());
        assertEquals(new CleanupSourceProfile(5, 2, 1), fast.profile());
        assertEquals(new CleanupSourceProfile(20, 7, 3), slow.profile());
    }

    @Test
    void duplicatePreservesBudgetWhileProfileUpdateStartsNewCycleWithoutResettingTask() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId source = source("profile_update");
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.refreshSourceBudgets(100);
        DimensionCleanupIndex.SourceRegistration registration = index.sponsor(OVERLAP.toLong(), true);
        registration.budget().consumeInspection();
        long retainedTask = index.nextTask();

        DimensionCleanupIndex.ActivationChanges duplicate =
                index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.refreshSourceBudgets(101);
        assertTrue(duplicate.newPassChunks().isEmpty());
        assertEquals(3, registration.budget().inspectionsRemaining());

        CleanupSourceProfile updated = new CleanupSourceProfile(7, 9, 5);
        DimensionCleanupIndex.ActivationChanges profileChange =
                index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.RESUME, updated);
        index.refreshSourceBudgets(102);

        assertTrue(profileChange.newPassChunks().isEmpty());
        assertEquals(9, registration.budget().inspectionsRemaining());
        assertEquals(updated, registration.profile());
        assertEquals(retainedTask, index.nextTask());
    }

    @Test
    void overlappingSourcesAlternateAsTaskSponsors() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        CleanupSourceProfile sponsorProfile = new CleanupSourceProfile(20, 4, 4);
        index.registerActive(source("first"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, sponsorProfile);
        index.registerActive(source("second"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, sponsorProfile);
        index.refreshSourceBudgets(0);

        SuppressionSourceId first = index.sponsor(OVERLAP.toLong(), true).sourceId();
        SuppressionSourceId second = index.sponsor(OVERLAP.toLong(), true).sourceId();
        SuppressionSourceId third = index.sponsor(OVERLAP.toLong(), true).sourceId();

        assertFalse(first.equals(second));
        assertEquals(first, third);
    }

    @Test
    void tenChunkTasksRotateWithoutStarvation() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        Set<ChunkPos> chunks = new HashSet<>();
        for (int i = 0; i < 10; i++) chunks.add(new ChunkPos(i, -i));
        index.registerActive(source("many"), chunks, CleanupActivationMode.NEW_PASS, PROFILE);

        Set<Long> visited = new HashSet<>();
        for (int i = 0; i < 10; i++) visited.add(index.nextTask());
        assertEquals(10, visited.size());
    }

    @Test
    void completedTaskLeavesQueueUntilExplicitNewPassResume() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        index.registerActive(source("complete"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS, PROFILE);
        index.suspendCompleted(OVERLAP.toLong());
        assertNull(index.nextTask());
        assertTrue(index.hasActiveSources(OVERLAP.toLong()));
        index.resumeIncomplete(OVERLAP.toLong());
        assertEquals(OVERLAP.toLong(), index.nextTask());
    }

    private static SuppressionSourceId source(String path) {
        return new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath("frontier_protocol", path));
    }
}
