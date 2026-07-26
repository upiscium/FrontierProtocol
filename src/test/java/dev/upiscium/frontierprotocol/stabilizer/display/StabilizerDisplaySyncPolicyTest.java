package dev.upiscium.frontierprotocol.stabilizer.display;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import org.junit.jupiter.api.Test;

class StabilizerDisplaySyncPolicyTest {
    @Test
    void dirtyChangesSyncOnceAndIdenticalSnapshotDoesNotResend() {
        StabilizerDisplaySyncPolicy policy = new StabilizerDisplaySyncPolicy();
        StabilizerDisplaySnapshot offline = snapshot(StabilizerStatus.OFFLINE, 0, 0);
        assertTrue(policy.shouldSync(10, offline));
        policy.recordSync(10, offline);
        policy.markDirty();
        policy.markDirty();
        assertFalse(policy.shouldSync(10, offline));

        StabilizerDisplaySnapshot inserted = snapshot(StabilizerStatus.OFFLINE, 1, 0);
        assertTrue(policy.shouldSync(11, inserted));
        policy.recordSync(11, inserted);
        assertFalse(policy.shouldSync(12, inserted));
    }

    @Test
    void activeAndGraceCountdownsSyncOnlyEveryTwentyTicks() {
        assertPeriodicCountdown(StabilizerStatus.ACTIVE);
        assertPeriodicCountdown(StabilizerStatus.GRACE_PERIOD);
    }

    @Test
    void unchangedOfflineCountdownDoesNotCausePeriodicSync() {
        StabilizerDisplaySyncPolicy policy = new StabilizerDisplaySyncPolicy();
        StabilizerDisplaySnapshot first = snapshot(StabilizerStatus.OFFLINE, 1, 100);
        policy.recordSync(0, first);
        assertFalse(policy.shouldSync(100, snapshot(StabilizerStatus.OFFLINE, 1, 99)));
    }

    @Test
    void statusAndInventoryChangesCanBeMarkedForNextTickSync() {
        StabilizerDisplaySyncPolicy policy = new StabilizerDisplaySyncPolicy();
        policy.recordSync(0, snapshot(StabilizerStatus.OFFLINE, 1, 0));
        policy.markDirty();
        assertTrue(policy.shouldSync(1, snapshot(StabilizerStatus.ACTIVE, 0, 6000)));
    }

    private static void assertPeriodicCountdown(StabilizerStatus status) {
        StabilizerDisplaySyncPolicy policy = new StabilizerDisplaySyncPolicy();
        policy.recordSync(100, snapshot(status, 0, 100));
        assertFalse(policy.shouldSync(119, snapshot(status, 0, 81)));
        assertTrue(policy.shouldSync(120, snapshot(status, 0, 80)));
    }

    private static StabilizerDisplaySnapshot snapshot(StabilizerStatus status, int cells, int remainingTicks) {
        return new StabilizerDisplaySnapshot(
                StabilizerTier.TIER_1, status, 32, 16.0, cells, 8, remainingTicks, 6000, 0, 0);
    }
}
