package dev.upiscium.frontierprotocol.ore;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

final class OreGenerationSuppressionPolicy {
    private OreGenerationSuppressionPolicy() {}

    static boolean isSuppressed(
            ResourceKey<Level> dimension,
            InitialSpawnOreSuppressionSnapshot snapshot,
            int chunkX,
            int chunkZ) {
        return dimension == Level.OVERWORLD && snapshot.contains(chunkX, chunkZ);
    }
}
