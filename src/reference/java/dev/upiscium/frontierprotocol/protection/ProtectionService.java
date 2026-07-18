package dev.upiscium.frontierprotocol.protection;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public interface ProtectionService {
    boolean isChunkProtected(ServerLevel level, ChunkPos chunkPos);

    boolean isBlockProtected(ServerLevel level, BlockPos blockPos);

    Optional<ProtectionSource> findSource(ServerLevel level, ChunkPos chunkPos);
}
