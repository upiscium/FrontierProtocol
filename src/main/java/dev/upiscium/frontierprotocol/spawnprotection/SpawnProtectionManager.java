package dev.upiscium.frontierprotocol.spawnprotection;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class SpawnProtectionManager {
    public static final SuppressionSourceId SOURCE_ID = new SuppressionSourceId(
            ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "initial_spawn"));
    public static final SuppressionSource SOURCE = new SuppressionSource(
            SOURCE_ID, SuppressionSourceType.INITIAL_SPAWN);

    private SpawnProtectionManager() {}

    public static void rebuild(ServerLevel overworld) {
        SpawnProtectionSavedData data = SpawnProtectionSavedData.get(overworld);
        data.initialize(
                new ChunkPos(overworld.getSharedSpawnPos()),
                FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.get(),
                FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.getAsInt());
        ServerInfectionSuppressionService service = ServerInfectionSuppressionService.INSTANCE;
        if (data.enabled()) {
            service.registerOrUpdateSource(overworld, SOURCE, coveredChunks(data.centerChunk(), data.radiusChunks()));
        } else {
            service.unregisterSource(overworld, SOURCE_ID);
        }
    }

    static Set<ChunkPos> coveredChunks(ChunkPos center, int radius) {
        int diameter = radius * 2 + 1;
        Set<ChunkPos> chunks = new HashSet<>(diameter * diameter);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long x = (long) center.x + dx;
                long z = (long) center.z + dz;
                if (x >= Integer.MIN_VALUE && x <= Integer.MAX_VALUE
                        && z >= Integer.MIN_VALUE && z <= Integer.MAX_VALUE) {
                    chunks.add(new ChunkPos((int) x, (int) z));
                }
            }
        }
        return Set.copyOf(chunks);
    }
}
