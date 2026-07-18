package dev.upiscium.frontierprotocol.protection;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class ProtectionIndex {
    private static final Map<ServerLevel, ProtectionIndex> BY_LEVEL = Collections.synchronizedMap(new IdentityHashMap<>());
    private final ServerLevel level;
    private final Map<ChunkPos, Set<StabilizationBeaconBlockEntity>> beaconsByChunk = new java.util.HashMap<>();

    private ProtectionIndex(ServerLevel level) {
        this.level = level;
    }

    public static ProtectionIndex get(ServerLevel level) {
        return BY_LEVEL.computeIfAbsent(level, ProtectionIndex::new);
    }

    public static void clear(ServerLevel level) {
        BY_LEVEL.remove(level);
    }

    public void register(StabilizationBeaconBlockEntity beacon) {
        requireServerThread();
        beaconsByChunk.computeIfAbsent(new ChunkPos(beacon.getBlockPos()), ignored -> new LinkedHashSet<>()).add(beacon);
    }

    public void unregister(StabilizationBeaconBlockEntity beacon) {
        requireServerThread();
        ChunkPos chunk = new ChunkPos(beacon.getBlockPos());
        Set<StabilizationBeaconBlockEntity> values = beaconsByChunk.get(chunk);
        if (values == null) return;
        values.remove(beacon);
        if (values.isEmpty()) beaconsByChunk.remove(chunk);
    }

    public Optional<StabilizationBeaconBlockEntity> findProtecting(ChunkPos target, int radius) {
        requireServerThread();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long sourceX = (long) target.x + dx;
                long sourceZ = (long) target.z + dz;
                if (sourceX < Integer.MIN_VALUE || sourceX > Integer.MAX_VALUE
                        || sourceZ < Integer.MIN_VALUE || sourceZ > Integer.MAX_VALUE) continue;
                ChunkPos sourceChunk = new ChunkPos((int) sourceX, (int) sourceZ);
                Set<StabilizationBeaconBlockEntity> values = beaconsByChunk.get(sourceChunk);
                if (values == null) continue;
                if (!level.hasChunk(sourceChunk.x, sourceChunk.z)) {
                    beaconsByChunk.remove(sourceChunk);
                    continue;
                }
                values.removeIf(beacon -> beacon.isRemoved() || level.getBlockEntity(beacon.getBlockPos()) != beacon);
                Optional<StabilizationBeaconBlockEntity> active = values.stream()
                        .filter(StabilizationBeaconBlockEntity::isProtecting).findFirst();
                if (values.isEmpty()) beaconsByChunk.remove(sourceChunk);
                if (active.isPresent()) return active;
            }
        }
        return Optional.empty();
    }

    private void requireServerThread() {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Protection index must be accessed on the server thread");
        }
    }
}
