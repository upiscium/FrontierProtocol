package dev.upiscium.frontierprotocol.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class CleanupCursorTest {
    @Test
    void mapsAll4096SectionPositionsWithoutDuplicates() {
        Set<BlockPos> positions = new HashSet<>();
        for (int index = 0; index < CleanupCursor.BLOCKS_PER_SECTION; index++) {
            CleanupCursor cursor = new CleanupCursor(0, index, false);
            positions.add(new BlockPos(cursor.localX(), cursor.localY(), cursor.localZ()));
        }
        assertEquals(CleanupCursor.BLOCKS_PER_SECTION, positions.size());
    }

    @Test
    void mapsBoundaryIndexesInXThenZThenYOrder() {
        assertEquals(new BlockPos(0, 0, 0), localPosition(0));
        assertEquals(new BlockPos(15, 0, 0), localPosition(15));
        assertEquals(new BlockPos(0, 0, 1), localPosition(16));
        assertEquals(new BlockPos(15, 15, 15), localPosition(4095));
    }

    @Test
    void advancesAcrossSectionBoundaryAndCompletesAtMaximumSection() {
        CleanupCursor nextSection = new CleanupCursor(0, 4095, false).advance(2);
        assertEquals(new CleanupCursor(1, 0, false), nextSection);

        CleanupCursor completed = new CleanupCursor(1, 4095, false).advance(2);
        assertTrue(completed.completed());
        assertEquals(1, completed.sectionIndex());
        assertEquals(4095, completed.localBlockIndex());
        assertEquals(completed, completed.advance(2));
    }

    @Test
    void mapsNegativeChunkAndMinimumSectionToWorldPosition() {
        CleanupCursor first = CleanupCursor.start();
        CleanupCursor last = new CleanupCursor(0, 4095, false);
        assertEquals(new BlockPos(-32, -64, -48), first.blockPos(new ChunkPos(-2, -3), -4));
        assertEquals(new BlockPos(-17, -49, -33), last.blockPos(new ChunkPos(-2, -3), -4));
    }

    @Test
    void rejectsInvalidCursorAndSectionRanges() {
        assertThrows(IllegalArgumentException.class, () -> new CleanupCursor(-1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new CleanupCursor(0, -1, false));
        assertThrows(IllegalArgumentException.class, () -> new CleanupCursor(0, 4096, false));
        assertThrows(IllegalArgumentException.class, () -> CleanupCursor.start().advance(0));
        assertThrows(IllegalArgumentException.class, () -> new CleanupCursor(2, 0, false).advance(2));
    }

    @Test
    void mutationBudgetDeferralDoesNotAdvanceCandidateCursor() {
        CleanupCursor cursor = new CleanupCursor(3, 144, false);
        assertEquals(cursor, cursor.afterInspection(8, true, false));
        assertEquals(new CleanupCursor(3, 145, false), cursor.afterInspection(8, true, true));
        assertEquals(new CleanupCursor(3, 145, false), cursor.afterInspection(8, false, false));
    }

    private static BlockPos localPosition(int index) {
        CleanupCursor cursor = new CleanupCursor(0, index, false);
        return new BlockPos(cursor.localX(), cursor.localY(), cursor.localZ());
    }
}
