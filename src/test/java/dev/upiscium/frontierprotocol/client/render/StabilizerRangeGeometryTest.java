package dev.upiscium.frontierprotocol.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class StabilizerRangeGeometryTest {
    @Test
    void computesRadiusZeroOneAndTwoBounds() {
        assertBounds(0, 1, 0, 16, 0, 16);
        assertBounds(1, 3, -16, 32, -16, 32);
        assertBounds(2, 5, -32, 48, -32, 48);
    }

    @Test
    void acceptsMaximumConfiguredRadius() {
        StabilizerRangeGeometry.RangeBounds bounds =
                StabilizerRangeGeometry.from(new ChunkPos(0, 0), 16, -64, 320);
        assertEquals(33, bounds.width());
        assertEquals(68, bounds.horizontalLines(64).size());
    }

    @Test
    void computesNegativeChunksAndExclusiveUpperBoundsWithoutShiftOverflow() {
        StabilizerRangeGeometry.RangeBounds negative =
                StabilizerRangeGeometry.from(new ChunkPos(-2, -3), 1, -64, 320);
        assertEquals(-48, negative.minBlockX());
        assertEquals(0, negative.maxBlockXExclusive());
        assertEquals(-64, negative.minBlockZ());
        assertEquals(-16, negative.maxBlockZExclusive());

        StabilizerRangeGeometry.RangeBounds edge =
                StabilizerRangeGeometry.from(new ChunkPos(Integer.MAX_VALUE, 0), 2, -64, 320);
        assertEquals(((long) Integer.MAX_VALUE + 3L) * 16L, edge.maxBlockXExclusive());
    }

    @Test
    void buildsOuterGridInternalLinesAndVerticalCornersWithoutDuplicates() {
        StabilizerRangeGeometry.RangeBounds bounds =
                StabilizerRangeGeometry.from(new ChunkPos(0, 0), 2, -64, 320);
        List<StabilizerRangeGeometry.LineSegment> horizontal = bounds.horizontalLines(70.02);
        List<StabilizerRangeGeometry.LineSegment> vertical = bounds.verticalCorners(32, 96);
        assertEquals(12, horizontal.size());
        assertEquals(4, horizontal.stream()
                .filter(line -> line.kind() == StabilizerRangeGeometry.LineKind.OUTER)
                .count());
        assertEquals(8, horizontal.stream()
                .filter(line -> line.kind() == StabilizerRangeGeometry.LineKind.INTERNAL)
                .count());
        assertEquals(4, vertical.size());
        assertEquals(horizontal.size(), new HashSet<>(horizontal).size());
        assertEquals(vertical.size(), new HashSet<>(vertical).size());
    }

    @Test
    void rejectsInvalidGeometry() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StabilizerRangeGeometry.from(new ChunkPos(0, 0), -1, -64, 320));
        assertThrows(
                IllegalArgumentException.class,
                () -> StabilizerRangeGeometry.from(new ChunkPos(0, 0), 17, -64, 320));
        assertThrows(
                IllegalArgumentException.class,
                () -> StabilizerRangeGeometry.from(new ChunkPos(0, 0), Integer.MAX_VALUE, -64, 320));
        assertThrows(
                IllegalArgumentException.class,
                () -> StabilizerRangeGeometry.from(new ChunkPos(0, 0), 0, 10, 10));
    }

    private static void assertBounds(
            int radius,
            int width,
            long minX,
            long maxX,
            long minZ,
            long maxZ) {
        StabilizerRangeGeometry.RangeBounds bounds =
                StabilizerRangeGeometry.from(new ChunkPos(0, 0), radius, -64, 320);
        assertEquals(width, bounds.width());
        assertEquals(minX, bounds.minBlockX());
        assertEquals(maxX, bounds.maxBlockXExclusive());
        assertEquals(minZ, bounds.minBlockZ());
        assertEquals(maxZ, bounds.maxBlockZExclusive());
    }
}
