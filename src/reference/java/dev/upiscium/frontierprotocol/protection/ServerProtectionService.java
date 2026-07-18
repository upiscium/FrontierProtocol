package dev.upiscium.frontierprotocol.protection;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class ServerProtectionService implements ProtectionService {
    public static final ServerProtectionService INSTANCE = new ServerProtectionService();

    private ServerProtectionService() {}

    @Override
    public boolean isChunkProtected(ServerLevel level, ChunkPos chunkPos) {
        return findSource(level, chunkPos).isPresent();
    }

    @Override
    public boolean isBlockProtected(ServerLevel level, BlockPos blockPos) {
        return isChunkProtected(level, new ChunkPos(blockPos));
    }

    @Override
    public Optional<ProtectionSource> findSource(ServerLevel level, ChunkPos chunkPos) {
        if (level != level.getServer().overworld()) return Optional.empty();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        ChunkPos origin = new ChunkPos(data.originChunkX(), data.originChunkZ());
        if (ProtectionGeometry.isWithinRadius(origin, chunkPos,
                FrontierProtocolServerConfig.INITIAL_PROTECTION_RADIUS.getAsInt())) {
            return Optional.of(ProtectionSource.initialSpawn());
        }
        level.getProfiler().push("frontier_protocol:protection_index");
        try {
            return ProtectionIndex.get(level)
                    .findProtecting(chunkPos, FrontierProtocolServerConfig.BEACON_RADIUS.getAsInt())
                    .map(beacon -> ProtectionSource.beacon(beacon.getBlockPos()));
        } finally {
            level.getProfiler().pop();
        }
    }
}
