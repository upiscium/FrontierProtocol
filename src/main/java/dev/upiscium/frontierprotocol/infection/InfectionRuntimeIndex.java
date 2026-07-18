package dev.upiscium.frontierprotocol.infection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;

public final class InfectionRuntimeIndex {
    private static final Map<ServerLevel, InfectionRuntimeIndex> BY_LEVEL = new WeakHashMap<>();

    private final Map<UUID, CarrierEntry> carriers = new HashMap<>();
    private final Map<Long, Set<UUID>> carriersByChunk = new HashMap<>();
    private final Map<UUID, InfectionNestBlockEntity> nests = new HashMap<>();
    private final Map<Long, InfectionNestBlockEntity> nestsByChunk = new HashMap<>();
    private final Map<UUID, Mob> nestMobs = new HashMap<>();
    private final Set<Long> persistentInfectionChunks = new HashSet<>();

    private InfectionRuntimeIndex() {}

    public static InfectionRuntimeIndex get(ServerLevel level) {
        return BY_LEVEL.computeIfAbsent(level, ignored -> new InfectionRuntimeIndex());
    }

    public static void clear(ServerLevel level) {
        BY_LEVEL.remove(level);
    }

    public void registerCarrier(Mob mob) {
        removeCarrier(mob.getUUID());
        long chunk = mob.chunkPosition().toLong();
        carriers.put(mob.getUUID(), new CarrierEntry(mob, chunk));
        carriersByChunk.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(mob.getUUID());
    }

    public void moveCarrier(UUID id, ChunkPos destination) {
        CarrierEntry entry = carriers.get(id);
        if (entry == null || entry.chunk == destination.toLong()) return;
        removeFromChunk(entry.chunk, id);
        entry.chunk = destination.toLong();
        carriersByChunk.computeIfAbsent(entry.chunk, ignored -> new HashSet<>()).add(id);
    }

    public void removeCarrier(UUID id) {
        CarrierEntry entry = carriers.remove(id);
        if (entry != null) removeFromChunk(entry.chunk, id);
    }

    public Collection<Mob> carriersIn(ChunkPos chunk) {
        Set<UUID> ids = carriersByChunk.get(chunk.toLong());
        if (ids == null) return java.util.List.of();
        ArrayList<Mob> result = new ArrayList<>(ids.size());
        for (UUID id : Set.copyOf(ids)) {
            CarrierEntry entry = carriers.get(id);
            if (entry == null || entry.mob.isRemoved() || !entry.mob.isAlive()) {
                removeCarrier(id);
            } else {
                result.add(entry.mob);
            }
        }
        return result;
    }

    public void registerNest(InfectionNestBlockEntity nest) {
        InfectionNestBlockEntity duplicate = nests.get(nest.nestId());
        if (duplicate != null && duplicate != nest && !duplicate.isRemoved()) {
            nest.replaceNestId(UUID.randomUUID());
        }
        nests.put(nest.nestId(), nest);
        long chunk = new ChunkPos(nest.getBlockPos()).toLong();
        nestsByChunk.put(chunk, nest);
        persistentInfectionChunks.add(chunk);
    }

    public void unregisterNest(InfectionNestBlockEntity nest) {
        nests.remove(nest.nestId(), nest);
        nestsByChunk.remove(new ChunkPos(nest.getBlockPos()).toLong(), nest);
    }

    public InfectionNestBlockEntity nestIn(ChunkPos chunk) {
        InfectionNestBlockEntity nest = nestsByChunk.get(chunk.toLong());
        return nest == null || nest.isRemoved() ? null : nest;
    }

    public void registerNestMob(Mob mob) {
        nestMobs.put(mob.getUUID(), mob);
    }

    public void removeNestMob(UUID id) {
        nestMobs.remove(id);
    }

    public boolean nestMobCapReached(net.minecraft.core.BlockPos pos, int radius, int localCap, int globalCap) {
        int local = 0;
        long radiusSquared = (long) radius * radius;
        for (var iterator = nestMobs.entrySet().iterator(); iterator.hasNext();) {
            Mob mob = iterator.next().getValue();
            if (mob.isRemoved() || !mob.isAlive()) {
                iterator.remove();
                continue;
            }
            if (mob.blockPosition().distSqr(pos) <= radiusSquared) local++;
        }
        return nestMobs.size() >= globalCap || local >= localCap;
    }

    public void markPersistentChunk(ChunkPos chunk, boolean active) {
        if (active) persistentInfectionChunks.add(chunk.toLong());
        else persistentInfectionChunks.remove(chunk.toLong());
    }

    public Set<Long> candidateChunks() {
        Set<Long> chunks = new HashSet<>(persistentInfectionChunks);
        chunks.addAll(carriersByChunk.keySet());
        return chunks;
    }

    private void removeFromChunk(long chunk, UUID id) {
        Set<UUID> ids = carriersByChunk.get(chunk);
        if (ids != null && ids.remove(id) && ids.isEmpty()) carriersByChunk.remove(chunk);
    }

    private static final class CarrierEntry {
        private final Mob mob;
        private long chunk;

        private CarrierEntry(Mob mob, long chunk) {
            this.mob = mob;
            this.chunk = chunk;
        }
    }
}
