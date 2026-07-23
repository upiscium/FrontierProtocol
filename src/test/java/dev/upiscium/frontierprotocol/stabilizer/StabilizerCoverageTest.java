package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class StabilizerCoverageTest {
    @Test
    void radiusZeroCoversOnlyCenter() {
        Set<ChunkPos> covered = StabilizerCoverage.coveredChunks(new ChunkPos(3, -4), 0);

        assertEquals(Set.of(new ChunkPos(3, -4)), covered);
    }

    @Test
    void radiusOneCoversCenterCornersAndNotOutside() {
        Set<ChunkPos> covered = StabilizerCoverage.coveredChunks(new ChunkPos(10, 20), 1);

        assertEquals(9, covered.size());
        assertTrue(covered.contains(new ChunkPos(10, 20)));
        assertTrue(covered.contains(new ChunkPos(9, 19)));
        assertTrue(covered.contains(new ChunkPos(11, 21)));
        assertFalse(covered.contains(new ChunkPos(12, 20)));
        assertFalse(covered.contains(new ChunkPos(10, 22)));
    }

    @Test
    void radiusTwoCoversExactFiveByFiveSquare() {
        Set<ChunkPos> covered = StabilizerCoverage.coveredChunks(new ChunkPos(-3, -7), 2);

        assertEquals(25, covered.size());
        assertTrue(covered.contains(new ChunkPos(-5, -9)));
        assertTrue(covered.contains(new ChunkPos(-1, -5)));
        assertFalse(covered.contains(new ChunkPos(0, -7)));
    }

    @Test
    void coverageUsesStableRowMajorOrderWithoutDuplicates() {
        Set<ChunkPos> covered = StabilizerCoverage.coveredChunks(new ChunkPos(0, 0), 1);

        assertEquals(
                List.of(
                        new ChunkPos(-1, -1),
                        new ChunkPos(0, -1),
                        new ChunkPos(1, -1),
                        new ChunkPos(-1, 0),
                        new ChunkPos(0, 0),
                        new ChunkPos(1, 0),
                        new ChunkPos(-1, 1),
                        new ChunkPos(0, 1),
                        new ChunkPos(1, 1)),
                new ArrayList<>(covered));
        assertEquals(covered.size(), covered.stream().distinct().count());
        assertThrows(UnsupportedOperationException.class, () -> covered.add(new ChunkPos(2, 2)));
    }

    @Test
    void blockPositionsUseFloorChunkCoordinatesAtBoundaries() {
        assertEquals(
                Set.of(new ChunkPos(0, 0)),
                StabilizerCoverage.coveredChunks(new BlockPos(15, 80, 15), 0));
        assertEquals(
                Set.of(new ChunkPos(1, 1)),
                StabilizerCoverage.coveredChunks(new BlockPos(16, -64, 16), 0));
        assertEquals(
                Set.of(new ChunkPos(-1, -1)),
                StabilizerCoverage.coveredChunks(new BlockPos(-1, 320, -1), 0));
        assertEquals(
                Set.of(new ChunkPos(-1, -1)),
                StabilizerCoverage.coveredChunks(new BlockPos(-16, 0, -16), 0));
        assertEquals(
                Set.of(new ChunkPos(-2, -2)),
                StabilizerCoverage.coveredChunks(new BlockPos(-17, 0, -17), 0));
    }

    @Test
    void rejectsNegativeRadius() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StabilizerCoverage.coveredChunks(new ChunkPos(0, 0), -1));
    }
}
