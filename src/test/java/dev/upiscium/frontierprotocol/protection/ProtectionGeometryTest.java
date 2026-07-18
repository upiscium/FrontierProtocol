package dev.upiscium.frontierprotocol.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ProtectionGeometryTest {
    @Test
    void initialProtectionHasExactFiveByFiveBoundary() {
        assertTrue(ProtectionGeometry.isWithinRadius(new ChunkPos(10, -4), new ChunkPos(12, -2), 2));
        assertTrue(ProtectionGeometry.isWithinRadius(new ChunkPos(10, -4), new ChunkPos(8, -6), 2));
        assertFalse(ProtectionGeometry.isWithinRadius(new ChunkPos(10, -4), new ChunkPos(13, -4), 2));
        assertFalse(ProtectionGeometry.isWithinRadius(new ChunkPos(10, -4), new ChunkPos(10, -7), 2));
    }

    @Test
    void beaconProtectionHasExactThreeByThreeBoundary() {
        assertTrue(ProtectionGeometry.isWithinRadius(new ChunkPos(-8, 5), new ChunkPos(-7, 6), 1));
        assertTrue(ProtectionGeometry.isWithinRadius(new ChunkPos(-8, 5), new ChunkPos(-9, 4), 1));
        assertFalse(ProtectionGeometry.isWithinRadius(new ChunkPos(-8, 5), new ChunkPos(-6, 5), 1));
        assertFalse(ProtectionGeometry.isWithinRadius(new ChunkPos(-8, 5), new ChunkPos(-8, 7), 1));
    }
}
