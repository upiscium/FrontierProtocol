package dev.upiscium.frontierprotocol.stabilizer.display;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerLimits;

public record StabilizerDisplaySnapshot(
        StabilizerTier tier,
        StabilizerStatus status,
        int minimumRpm,
        double stressImpact,
        int cellCount,
        int cellCapacity,
        int cellRemainingTicks,
        int cellDurationTicks,
        int graceRemainingTicks,
        int chunkRadius) {
    public StabilizerDisplaySnapshot {
        if (tier == null) throw new IllegalArgumentException("tier must not be null");
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (minimumRpm < 1) throw new IllegalArgumentException("minimumRpm must be at least 1");
        if (!Double.isFinite(stressImpact) || stressImpact < 0.0) {
            throw new IllegalArgumentException("stressImpact must be finite and non-negative");
        }
        if (cellCount < 0 || cellCount > StabilizerLimits.MAX_CELL_CAPACITY) {
            throw new IllegalArgumentException("cellCount must be between 0 and 64");
        }
        if (cellCapacity < StabilizerLimits.MIN_CELL_CAPACITY
                || cellCapacity > StabilizerLimits.MAX_CELL_CAPACITY) {
            throw new IllegalArgumentException("cellCapacity must be between 1 and 64");
        }
        if (cellRemainingTicks < 0) throw new IllegalArgumentException("cellRemainingTicks must not be negative");
        if (cellDurationTicks < 1) throw new IllegalArgumentException("cellDurationTicks must be at least 1");
        if (graceRemainingTicks < 0) throw new IllegalArgumentException("graceRemainingTicks must not be negative");
        if (chunkRadius < StabilizerLimits.MIN_CHUNK_RADIUS
                || chunkRadius > StabilizerLimits.MAX_CHUNK_RADIUS) {
            throw new IllegalArgumentException("chunkRadius must be between 0 and 16");
        }
    }

    public int coverageWidth() {
        return Math.addExact(Math.multiplyExact(chunkRadius, 2), 1);
    }

    public int coverageChunkCount() {
        return Math.multiplyExact(coverageWidth(), coverageWidth());
    }

    public int suppressedChunkCount() {
        return status.suppressesInfection() ? coverageChunkCount() : 0;
    }

    public boolean operationallyEquals(StabilizerDisplaySnapshot other) {
        return other != null
                && tier == other.tier
                && status == other.status
                && minimumRpm == other.minimumRpm
                && Double.compare(stressImpact, other.stressImpact) == 0
                && cellCount == other.cellCount
                && cellCapacity == other.cellCapacity
                && cellDurationTicks == other.cellDurationTicks
                && chunkRadius == other.chunkRadius;
    }
}
