package dev.upiscium.frontierprotocol.sector;

import net.minecraft.world.level.ChunkPos;

public record SectorPos(int x, int z) implements Comparable<SectorPos> {
    public static SectorPos fromChunk(ChunkPos chunk, int sectorSize) {
        if (sectorSize < 1) {
            throw new IllegalArgumentException("sectorSize must be positive");
        }
        return new SectorPos(Math.floorDiv(chunk.x, sectorSize), Math.floorDiv(chunk.z, sectorSize));
    }

    public long chebyshevDistance(SectorPos other) {
        long dx = Math.abs((long) x - other.x);
        long dz = Math.abs((long) z - other.z);
        return Math.max(dx, dz);
    }

    @Override
    public int compareTo(SectorPos other) {
        int xResult = Integer.compare(x, other.x);
        return xResult != 0 ? xResult : Integer.compare(z, other.z);
    }
}
