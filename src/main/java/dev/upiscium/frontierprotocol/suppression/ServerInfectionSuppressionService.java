package dev.upiscium.frontierprotocol.suppression;

import dev.upiscium.frontierprotocol.api.suppression.InfectionSuppressionService;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class ServerInfectionSuppressionService implements InfectionSuppressionService {
    public static final ServerInfectionSuppressionService INSTANCE = new ServerInfectionSuppressionService();

    private final Map<ServerLevel, DimensionSuppressionIndex> indexes =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private ServerInfectionSuppressionService() {}

    @Override
    public boolean isSuppressed(ServerLevel level, BlockPos pos) {
        return isSuppressed(level, new ChunkPos(pos));
    }

    @Override
    public boolean isSuppressed(ServerLevel level, ChunkPos chunkPos) {
        requireServerThread(level);
        DimensionSuppressionIndex index = indexes.get(level);
        return index != null && index.isSuppressed(chunkPos);
    }

    @Override
    public Set<SuppressionSource> getSources(ServerLevel level, ChunkPos chunkPos) {
        requireServerThread(level);
        DimensionSuppressionIndex index = indexes.get(level);
        return index == null ? Set.of() : index.getSources(chunkPos);
    }

    public void registerOrUpdateSource(ServerLevel level, SuppressionSource source, Set<ChunkPos> chunks) {
        requireServerThread(level);
        indexes.computeIfAbsent(level, ignored -> new DimensionSuppressionIndex()).registerOrUpdate(source, chunks);
    }

    public void unregisterSource(ServerLevel level, SuppressionSourceId sourceId) {
        requireServerThread(level);
        DimensionSuppressionIndex index = indexes.get(level);
        if (index != null) index.unregister(sourceId);
    }

    public void clear(ServerLevel level) {
        requireServerThread(level);
        indexes.remove(level);
    }

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Suppression index must be accessed on the server thread");
        }
    }
}
