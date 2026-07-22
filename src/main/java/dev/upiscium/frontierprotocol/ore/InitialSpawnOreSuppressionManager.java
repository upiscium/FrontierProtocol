package dev.upiscium.frontierprotocol.ore;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

public final class InitialSpawnOreSuppressionManager {
    private static final InitialSpawnOreSuppressionSnapshots<MinecraftServer> SNAPSHOTS =
            new InitialSpawnOreSuppressionSnapshots<>();

    private InitialSpawnOreSuppressionManager() {}

    public static void publishProvisional(ServerLevel level, ChunkPos center, int searchRadiusChunks) {
        requireServerThread(level);
        int configuredRadius = FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_RADIUS_CHUNKS.getAsInt();
        int provisionalRadius = Math.addExact(configuredRadius, searchRadiusChunks);
        publish(level.getServer(), center, provisionalRadius);
        if (FrontierProtocolServerConfig.DEBUG_LOGGING.get()) {
            FrontierProtocolMod.LOGGER.info(
                    "Published provisional initial-spawn ore snapshot at chunk [{}, {}] with radius {}",
                    center.x,
                    center.z,
                    provisionalRadius);
        }
    }

    public static void initializeFinalSnapshot(ServerLevel level, ChunkPos initialSpawn) {
        requireServerThread(level);
        SpawnProtectionSavedData data = SpawnProtectionSavedData.get(level);
        data.initialize(initialSpawn);
        rebuildSnapshot(level);
        if (FrontierProtocolServerConfig.DEBUG_LOGGING.get()) {
            ChunkPos center = data.centerChunk();
            FrontierProtocolMod.LOGGER.info(
                    "Published final initial-spawn ore snapshot at chunk [{}, {}] with radius {}",
                    center.x,
                    center.z,
                    FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_RADIUS_CHUNKS.getAsInt());
        }
    }

    public static void rebuildSnapshot(ServerLevel level) {
        requireServerThread(level);
        SpawnProtectionSavedData data = SpawnProtectionSavedData.get(level);
        if (!data.initialized()) {
            SNAPSHOTS.put(level.getServer(), InitialSpawnOreSuppressionSnapshot.uninitialized());
            return;
        }
        publish(level.getServer(), data.centerChunk(),
                FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_RADIUS_CHUNKS.getAsInt());
    }

    static InitialSpawnOreSuppressionSnapshot snapshot(MinecraftServer server) {
        return SNAPSHOTS.get(server);
    }

    public static boolean isSuppressed(WorldGenLevel level, BlockPos origin) {
        ChunkPos chunk = new ChunkPos(origin);
        ServerLevel serverLevel = level.getLevel();
        return OreGenerationSuppressionPolicy.isSuppressed(
                serverLevel.dimension(), snapshot(serverLevel.getServer()), chunk.x, chunk.z);
    }

    public static void clear(MinecraftServer server) {
        SNAPSHOTS.clear(server);
    }

    private static void publish(MinecraftServer server, ChunkPos center, int radius) {
        SNAPSHOTS.put(server, new InitialSpawnOreSuppressionSnapshot(
                true,
                FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_ENABLED.get(),
                center.x,
                center.z,
                radius));
    }

    private static void requireServerThread(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            throw new IllegalArgumentException("Initial spawn ore suppression is Overworld-only");
        }
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Initial spawn ore snapshot updates require the server thread");
        }
    }
}
