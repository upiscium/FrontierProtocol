package dev.upiscium.frontierprotocol.breach;

import net.minecraft.world.level.block.state.BlockState;

public final class BreachRules {
    private BreachRules() {}

    public static boolean hardnessAllowed(float hardness, double maximum) {
        return Float.isFinite(hardness) && hardness >= 0.0F && hardness <= maximum;
    }

    public static boolean hasNoBlockEntity(BlockState state) {
        return !state.hasBlockEntity();
    }

    public static int breakDurationTicks(float hardness, double multiplier) {
        if (!Float.isFinite(hardness) || hardness < 0.0F || !Double.isFinite(multiplier) || multiplier <= 0.0) {
            throw new IllegalArgumentException("Hardness and multiplier must be finite and non-negative");
        }
        return Math.max(1, (int) Math.ceil((20.0 + hardness * 40.0) * multiplier));
    }

    public static int progressStage(int elapsedTicks, int durationTicks) {
        if (durationTicks <= 0) throw new IllegalArgumentException("Duration must be positive");
        return Math.min(9, Math.max(0, elapsedTicks * 10 / durationTicks));
    }
}
