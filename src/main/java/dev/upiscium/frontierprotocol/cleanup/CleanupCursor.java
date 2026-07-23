package dev.upiscium.frontierprotocol.cleanup;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public record CleanupCursor(int sectionIndex, int localBlockIndex, boolean completed) {
    public static final int BLOCKS_PER_SECTION = 4096;

    public CleanupCursor {
        if (sectionIndex < 0) throw new IllegalArgumentException("Cleanup section index cannot be negative");
        if (localBlockIndex < 0 || localBlockIndex >= BLOCKS_PER_SECTION) {
            throw new IllegalArgumentException("Cleanup local block index must be between 0 and 4095");
        }
    }

    public static CleanupCursor start() {
        return new CleanupCursor(0, 0, false);
    }

    public int localX() {
        return localBlockIndex & 15;
    }

    public int localZ() {
        return (localBlockIndex >>> 4) & 15;
    }

    public int localY() {
        return (localBlockIndex >>> 8) & 15;
    }

    public BlockPos blockPos(ChunkPos chunk, int minSection) {
        return new BlockPos(
                chunk.getMinBlockX() + localX(),
                ((minSection + sectionIndex) << 4) + localY(),
                chunk.getMinBlockZ() + localZ());
    }

    public CleanupCursor advance(int sectionCount) {
        if (sectionCount <= 0 || sectionIndex >= sectionCount) {
            throw new IllegalArgumentException("Cleanup cursor is outside the dimension section range");
        }
        if (completed) return this;
        if (localBlockIndex + 1 < BLOCKS_PER_SECTION) {
            return new CleanupCursor(sectionIndex, localBlockIndex + 1, false);
        }
        if (sectionIndex + 1 < sectionCount) {
            return new CleanupCursor(sectionIndex + 1, 0, false);
        }
        return new CleanupCursor(sectionIndex, localBlockIndex, true);
    }

    public CleanupCursor afterInspection(
            int sectionCount, boolean cleanupCandidate, boolean mutationSucceeded) {
        return cleanupCandidate && !mutationSucceeded ? this : advance(sectionCount);
    }
}
