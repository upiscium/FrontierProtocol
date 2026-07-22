package dev.upiscium.frontierprotocol.ore;

/** Immutable worldgen-safe view of initial spawn ore suppression. */
public record InitialSpawnOreSuppressionSnapshot(
        boolean initialized, boolean enabled, int centerChunkX, int centerChunkZ, int radiusChunks) {
    public static InitialSpawnOreSuppressionSnapshot uninitialized() {
        return new InitialSpawnOreSuppressionSnapshot(false, false, 0, 0, 0);
    }

    public InitialSpawnOreSuppressionSnapshot {
        if (radiusChunks < 0) throw new IllegalArgumentException("radiusChunks must be non-negative");
    }

    public boolean contains(int chunkX, int chunkZ) {
        if (!initialized || !enabled) return false;
        long dx = (long) chunkX - centerChunkX;
        long dz = (long) chunkZ - centerChunkZ;
        return dx >= -radiusChunks && dx <= radiusChunks
                && dz >= -radiusChunks && dz <= radiusChunks;
    }
}
