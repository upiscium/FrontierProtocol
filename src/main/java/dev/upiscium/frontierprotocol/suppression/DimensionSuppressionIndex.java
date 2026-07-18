package dev.upiscium.frontierprotocol.suppression;

import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;

final class DimensionSuppressionIndex {
    private final Map<Long, Set<SuppressionSource>> sourcesByChunk = new HashMap<>();
    private final Map<SuppressionSourceId, SourceRegistration> registrationsById = new HashMap<>();

    public void registerOrUpdate(SuppressionSource source, Set<ChunkPos> chunks) {
        unregister(source.id());
        Set<Long> chunkKeys = new HashSet<>(chunks.size());
        for (ChunkPos chunk : chunks) {
            long key = chunk.toLong();
            chunkKeys.add(key);
            sourcesByChunk.computeIfAbsent(key, ignored -> new HashSet<>()).add(source);
        }
        registrationsById.put(source.id(), new SourceRegistration(source, Set.copyOf(chunkKeys)));
    }

    public void unregister(SuppressionSourceId sourceId) {
        SourceRegistration registration = registrationsById.remove(sourceId);
        if (registration == null) return;
        for (long chunkKey : registration.chunkKeys()) {
            Set<SuppressionSource> sources = sourcesByChunk.get(chunkKey);
            if (sources == null) continue;
            sources.remove(registration.source());
            if (sources.isEmpty()) sourcesByChunk.remove(chunkKey);
        }
    }

    public boolean isSuppressed(ChunkPos chunkPos) {
        return sourcesByChunk.containsKey(chunkPos.toLong());
    }

    public Set<SuppressionSource> getSources(ChunkPos chunkPos) {
        Set<SuppressionSource> sources = sourcesByChunk.get(chunkPos.toLong());
        return sources == null ? Set.of() : Set.copyOf(sources);
    }

    private record SourceRegistration(SuppressionSource source, Set<Long> chunkKeys) {}
}
