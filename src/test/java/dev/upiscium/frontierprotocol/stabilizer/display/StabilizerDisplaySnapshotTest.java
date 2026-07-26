package dev.upiscium.frontierprotocol.stabilizer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import org.junit.jupiter.api.Test;

class StabilizerDisplaySnapshotTest {
    @Test
    void acceptsAllTiersStatusesAndCapacityBounds() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            for (StabilizerStatus status : StabilizerStatus.values()) {
                assertEquals(tier, snapshot(tier, status, 0, 1).tier());
            }
        }
        assertEquals(64, snapshot(StabilizerTier.TIER_3, StabilizerStatus.ACTIVE, 64, 64).cellCount());
    }

    @Test
    void acceptsInventoryAboveCurrentConfiguredCapacity() {
        StabilizerDisplaySnapshot snapshot = new StabilizerDisplaySnapshot(
                StabilizerTier.TIER_2,
                StabilizerStatus.OFFLINE,
                64,
                64.0,
                32,
                8,
                0,
                3000,
                0,
                1);

        assertEquals(32, snapshot.cellCount());
        assertEquals(8, snapshot.cellCapacity());
    }

    @Test
    void acceptsConfiguredChunkRadiusBounds() {
        StabilizerDisplaySnapshot minimum =
                new StabilizerDisplaySnapshot(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 1, 0, 1, 0, 0);
        StabilizerDisplaySnapshot maximum =
                new StabilizerDisplaySnapshot(StabilizerTier.TIER_3, StabilizerStatus.ACTIVE, 1, 0.0, 0, 1, 1, 1, 0, 16);
        assertEquals(0, minimum.chunkRadius());
        assertEquals(16, maximum.chunkRadius());
        assertEquals(33, maximum.coverageWidth());
        assertEquals(1089, maximum.coverageChunkCount());
    }

    @Test
    void rejectsInvalidValues() {
        assertInvalid(null, StabilizerStatus.OFFLINE, 1, 0.0, 0, 1, 0, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, null, 1, 0.0, 0, 1, 0, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 0, 0.0, 0, 1, 0, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, Double.NaN, 0, 1, 0, 1, 0, 0);
        assertInvalid(
                StabilizerTier.TIER_1,
                StabilizerStatus.OFFLINE,
                1,
                Double.POSITIVE_INFINITY,
                0,
                1,
                0,
                1,
                0,
                0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, -1, 1, 0, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 65, 64, 0, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 0, 0, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 65, 0, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 1, -1, 1, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 1, 0, 0, 0, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 1, 0, 1, -1, 0);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 1, 0, 1, 0, -1);
        assertInvalid(StabilizerTier.TIER_1, StabilizerStatus.OFFLINE, 1, 0.0, 0, 1, 0, 1, 0, 17);
        assertInvalid(
                StabilizerTier.TIER_1,
                StabilizerStatus.OFFLINE,
                1,
                0.0,
                0,
                1,
                0,
                1,
                0,
                Integer.MAX_VALUE);
    }

    private static StabilizerDisplaySnapshot snapshot(
            StabilizerTier tier, StabilizerStatus status, int count, int capacity) {
        return new StabilizerDisplaySnapshot(tier, status, 32, 16.0, count, capacity, 0, 6000, 0, 0);
    }

    private static void assertInvalid(
            StabilizerTier tier,
            StabilizerStatus status,
            int minimumRpm,
            double stressImpact,
            int cellCount,
            int cellCapacity,
            int cellRemainingTicks,
            int cellDurationTicks,
            int graceRemainingTicks,
            int chunkRadius) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StabilizerDisplaySnapshot(
                        tier,
                        status,
                        minimumRpm,
                        stressImpact,
                        cellCount,
                        cellCapacity,
                        cellRemainingTicks,
                        cellDurationTicks,
                        graceRemainingTicks,
                        chunkRadius));
    }
}
