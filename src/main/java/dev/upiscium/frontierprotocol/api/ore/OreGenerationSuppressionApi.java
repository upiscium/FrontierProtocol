package dev.upiscium.frontierprotocol.api.ore;

import dev.upiscium.frontierprotocol.ore.InitialSpawnOreSuppressionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Worldgen-thread-safe compatibility query for initial spawn ore suppression.
 * Standard {@code OreFeature} implementations are handled automatically; custom ore features must call this API.
 */
public final class OreGenerationSuppressionApi {
    private OreGenerationSuppressionApi() {}

    public static boolean isSuppressed(WorldGenLevel level, BlockPos origin) {
        return InitialSpawnOreSuppressionManager.isSuppressed(level, origin);
    }
}
