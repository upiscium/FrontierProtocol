package dev.upiscium.frontierprotocol.stabilizer;

import dev.upiscium.frontierprotocol.cleanup.CleanupSourceProfile;

public record StabilizerTierDefinition(
        StabilizerTier tier,
        int chunkRadius,
        int minimumRpm,
        double stressImpact,
        int cellCapacity,
        int cellDurationTicks,
        int gracePeriodTicks,
        CleanupSourceProfile cleanupProfile) {
    public StabilizerTierDefinition {
        if (tier == null) throw new IllegalArgumentException("tier must not be null");
        if (chunkRadius < StabilizerLimits.MIN_CHUNK_RADIUS
                || chunkRadius > StabilizerLimits.MAX_CHUNK_RADIUS) {
            throw new IllegalArgumentException("chunkRadius must be between 0 and 16");
        }
        if (minimumRpm < 1) throw new IllegalArgumentException("minimumRpm must be at least 1");
        if (!Double.isFinite(stressImpact) || stressImpact < 0.0) {
            throw new IllegalArgumentException("stressImpact must be finite and non-negative");
        }
        if (cellCapacity < StabilizerLimits.MIN_CELL_CAPACITY
                || cellCapacity > StabilizerLimits.MAX_CELL_CAPACITY) {
            throw new IllegalArgumentException("cellCapacity must be between 1 and 64");
        }
        if (cellDurationTicks < 1) throw new IllegalArgumentException("cellDurationTicks must be at least 1");
        if (gracePeriodTicks < 0) throw new IllegalArgumentException("gracePeriodTicks must not be negative");
        if (cleanupProfile == null) throw new IllegalArgumentException("cleanupProfile must not be null");
    }
}
