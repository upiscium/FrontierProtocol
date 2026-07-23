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

    @Test
    void overlapCreatesOneTaskAndOneSourceRemovalKeepsItActive() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        index.registerActive(source("a"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
        index.registerActive(source("b"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);

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
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
        index.pause(source);

        assertEquals(0, index.activeTaskCount());
        assertTrue(index.hasRegistrationCovering(OVERLAP.toLong()));
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.RESUME);

        index.refreshSourceBudgets(20, 20, 3, 1);
        DimensionCleanupIndex.SourceRegistration sponsor = index.sponsor(OVERLAP.toLong(), true);
        assertEquals(CleanupActivationMode.RESUME, sponsor.activationMode());
        assertEquals(3, sponsor.budget().inspectionsRemaining());
    }

    @Test
    void deactivatingFinalPausedSourceRequiresRestartButOverlapDoesNot() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        SuppressionSourceId first = source("paused_first");
        SuppressionSourceId second = source("paused_second");
        index.registerActive(first, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
        index.registerActive(second, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
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
        index.registerActive(source, Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
        DimensionCleanupIndex.ActivationChanges changes =
                index.registerActive(source, Set.of(next), CleanupActivationMode.NEW_PASS);

        assertEquals(Set.of(OVERLAP.toLong()), changes.newlyInactive());
        assertEquals(Set.of(next.toLong()), changes.newlyActive());
        assertEquals(next.toLong(), index.nextTask());
    }

    @Test
    void sourceBudgetsRefreshOnlyAtConfiguredCycleBoundary() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        index.registerActive(source("budget"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
        index.refreshSourceBudgets(100, 20, 2, 1);
        DimensionCleanupIndex.SourceRegistration sponsor = index.sponsor(OVERLAP.toLong(), true);
        sponsor.budget().consumeInspection();
        sponsor.budget().consumeMutation();

        index.refreshSourceBudgets(119, 20, 2, 1);
        assertEquals(1, sponsor.budget().inspectionsRemaining());
        assertEquals(0, sponsor.budget().mutationsRemaining());
        index.refreshSourceBudgets(120, 20, 2, 1);
        assertEquals(2, sponsor.budget().inspectionsRemaining());
        assertEquals(1, sponsor.budget().mutationsRemaining());
    }

    @Test
    void overlappingSourcesAlternateAsTaskSponsors() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        index.registerActive(source("first"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
        index.registerActive(source("second"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
        index.refreshSourceBudgets(0, 20, 4, 4);

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
        index.registerActive(source("many"), chunks, CleanupActivationMode.NEW_PASS);

        Set<Long> visited = new HashSet<>();
        for (int i = 0; i < 10; i++) visited.add(index.nextTask());
        assertEquals(10, visited.size());
    }

    @Test
    void completedTaskLeavesQueueUntilExplicitNewPassResume() {
        DimensionCleanupIndex index = new DimensionCleanupIndex();
        index.registerActive(source("complete"), Set.of(OVERLAP), CleanupActivationMode.NEW_PASS);
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
