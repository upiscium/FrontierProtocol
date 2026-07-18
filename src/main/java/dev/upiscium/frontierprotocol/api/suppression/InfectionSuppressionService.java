package dev.upiscium.frontierprotocol.api.suppression;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public interface InfectionSuppressionService {
    boolean isSuppressed(ServerLevel level, BlockPos pos);

    boolean isSuppressed(ServerLevel level, ChunkPos chunkPos);

    Set<SuppressionSource> getSources(ServerLevel level, ChunkPos chunkPos);
}
