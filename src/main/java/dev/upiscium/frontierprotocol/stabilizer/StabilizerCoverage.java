package dev.upiscium.frontierprotocol.stabilizer;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class StabilizerCoverage {
    private StabilizerCoverage() {}

    public static Set<ChunkPos> coveredChunks(BlockPos stabilizerPos, int chunkRadius) {
        if (stabilizerPos == null) throw new IllegalArgumentException("stabilizerPos must not be null");
        return coveredChunks(new ChunkPos(stabilizerPos), chunkRadius);
    }

    public static Set<ChunkPos> coveredChunks(ChunkPos center, int chunkRadius) {
        if (center == null) throw new IllegalArgumentException("center must not be null");
        if (chunkRadius < 0) throw new IllegalArgumentException("chunkRadius must not be negative");

        int minX = Math.subtractExact(center.x, chunkRadius);
        int maxX = Math.addExact(center.x, chunkRadius);
        int minZ = Math.subtractExact(center.z, chunkRadius);
        int maxZ = Math.addExact(center.z, chunkRadius);
        LinkedHashSet<ChunkPos> covered = new LinkedHashSet<>();
        for (long z = minZ; z <= maxZ; z++) {
            for (long x = minX; x <= maxX; x++) {
                covered.add(new ChunkPos((int) x, (int) z));
            }
        }
        return Collections.unmodifiableSet(covered);
    }
}
