package dev.upiscium.frontierprotocol.sector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class SectorPosTest {
    @Test
    void convertsNegativeChunkCoordinatesWithFloorDivision() {
        assertEquals(new SectorPos(-1, -1), SectorPos.fromChunk(new ChunkPos(-1, -1), 8));
        assertEquals(new SectorPos(-1, -1), SectorPos.fromChunk(new ChunkPos(-8, -8), 8));
        assertEquals(new SectorPos(-2, -2), SectorPos.fromChunk(new ChunkPos(-9, -9), 8));
        assertEquals(new SectorPos(0, 0), SectorPos.fromChunk(new ChunkPos(7, 7), 8));
        assertEquals(new SectorPos(1, 1), SectorPos.fromChunk(new ChunkPos(8, 8), 8));
    }

    @Test
    void computesChebyshevDistanceWithoutIntegerOverflow() {
        assertEquals(7L, new SectorPos(-3, 4).chebyshevDistance(new SectorPos(4, -1)));
        assertEquals(4294967295L, new SectorPos(Integer.MIN_VALUE, 0)
                .chebyshevDistance(new SectorPos(Integer.MAX_VALUE, 0)));
    }
}
