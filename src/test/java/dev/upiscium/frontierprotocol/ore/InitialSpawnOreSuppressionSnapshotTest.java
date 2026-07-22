package dev.upiscium.frontierprotocol.ore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class InitialSpawnOreSuppressionSnapshotTest {
    @Test
    void radiusZeroCoversOnlyCenter() {
        InitialSpawnOreSuppressionSnapshot snapshot = snapshot(4, -7, 0);
        assertTrue(snapshot.contains(4, -7));
        assertFalse(snapshot.contains(5, -7));
    }

    @Test
    void radiiOneAndTwoHaveInclusiveSquareBoundaries() {
        assertTrue(snapshot(-4, 8, 1).contains(-5, 9));
        assertFalse(snapshot(-4, 8, 1).contains(-6, 8));
        assertTrue(snapshot(-4, 8, 2).contains(-6, 6));
        assertFalse(snapshot(-4, 8, 2).contains(-7, 8));
    }

    @Test
    void longDifferencesDoNotOverflowAtIntegerExtremes() {
        InitialSpawnOreSuppressionSnapshot minimum = snapshot(Integer.MIN_VALUE, Integer.MIN_VALUE, 2);
        InitialSpawnOreSuppressionSnapshot maximum = snapshot(Integer.MAX_VALUE, Integer.MAX_VALUE, 2);
        assertFalse(minimum.contains(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertFalse(maximum.contains(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertTrue(minimum.contains(Integer.MIN_VALUE + 2, Integer.MIN_VALUE + 2));
        assertTrue(maximum.contains(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 2));
    }

    @Test
    void disabledAndUninitializedSnapshotsFailOpen() {
        assertFalse(new InitialSpawnOreSuppressionSnapshot(true, false, 0, 0, 2).contains(0, 0));
        assertFalse(InitialSpawnOreSuppressionSnapshot.uninitialized().contains(0, 0));
    }

    @Test
    void policyIsOverworldOnlyAndIgnoresY() {
        InitialSpawnOreSuppressionSnapshot snapshot = snapshot(-2, -3, 1);
        assertTrue(OreGenerationSuppressionPolicy.isSuppressed(Level.OVERWORLD, snapshot, -2, -3));
        assertFalse(OreGenerationSuppressionPolicy.isSuppressed(Level.NETHER, snapshot, -2, -3));
    }

    @Test
    void registriesSeparateServersUpdateRadiusAndClear() {
        Object firstServer = new Object();
        Object secondServer = new Object();
        InitialSpawnOreSuppressionSnapshots<Object> snapshots = new InitialSpawnOreSuppressionSnapshots<>();
        InitialSpawnOreSuppressionSnapshot first = snapshot(1, 2, 0);
        InitialSpawnOreSuppressionSnapshot second = snapshot(1, 2, 2);
        snapshots.put(firstServer, first);
        snapshots.put(secondServer, second);
        assertSame(first, snapshots.get(firstServer));
        assertSame(second, snapshots.get(secondServer));
        snapshots.put(firstServer, second);
        assertTrue(snapshots.get(firstServer).contains(3, 4));
        snapshots.clear(firstServer);
        assertFalse(snapshots.get(firstServer).initialized());
        assertSame(second, snapshots.get(secondServer));
    }

    private static InitialSpawnOreSuppressionSnapshot snapshot(int x, int z, int radius) {
        return new InitialSpawnOreSuppressionSnapshot(true, true, x, z, radius);
    }
}
