package dev.upiscium.frontierprotocol.compat.spore;

import dev.upiscium.frontierprotocol.api.suppression.InfectionSuppressionApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class SporeSuppressionQueries {
    private SporeSuppressionQueries() {}

    public static boolean isSuppressed(Level level, BlockPos target) {
        return level instanceof ServerLevel serverLevel
                && InfectionSuppressionApi.get().isSuppressed(serverLevel, target);
    }
}
