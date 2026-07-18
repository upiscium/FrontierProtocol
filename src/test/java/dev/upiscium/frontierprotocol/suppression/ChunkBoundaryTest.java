package dev.upiscium.frontierprotocol.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ChunkBoundaryTest {
    @Test
    void blockPositionsUseFloorBasedChunksAcrossZero() {
        assertEquals(new ChunkPos(-1, -1), new ChunkPos(new BlockPos(-1, 0, -1)));
        assertEquals(new ChunkPos(-1, -1), new ChunkPos(new BlockPos(-16, 0, -16)));
        assertEquals(new ChunkPos(-2, -2), new ChunkPos(new BlockPos(-17, 0, -17)));
        assertEquals(new ChunkPos(0, 0), new ChunkPos(new BlockPos(0, 0, 0)));
        assertEquals(new ChunkPos(0, 0), new ChunkPos(new BlockPos(15, 0, 15)));
        assertEquals(new ChunkPos(1, 1), new ChunkPos(new BlockPos(16, 0, 16)));
    }
}
