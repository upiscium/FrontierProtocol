package dev.upiscium.frontierprotocol.stabilizer;

import dev.upiscium.frontierprotocol.cleanup.CleanupSourceProfile;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;

public final class StabilizerTierDefinitions {
    private StabilizerTierDefinitions() {}

    public static StabilizerTierDefinition resolve(StabilizerTier tier) {
        if (tier == null) throw new IllegalArgumentException("tier must not be null");
        return switch (tier) {
            case TIER_1 -> new StabilizerTierDefinition(
                    tier,
                    FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.getAsInt(),
                    FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.getAsInt(),
                    FrontierProtocolServerConfig.TIER1_STRESS_IMPACT.getAsDouble(),
                    FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.getAsInt(),
                    FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.getAsInt(),
                    FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.getAsInt(),
                    new CleanupSourceProfile(
                            FrontierProtocolServerConfig.TIER1_CLEANUP_INTERVAL_TICKS.getAsInt(),
                            FrontierProtocolServerConfig.TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.getAsInt(),
                            FrontierProtocolServerConfig.TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE.getAsInt()));
            case TIER_2 -> new StabilizerTierDefinition(
                    tier,
                    FrontierProtocolServerConfig.TIER2_CHUNK_RADIUS.getAsInt(),
                    FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.getAsInt(),
                    FrontierProtocolServerConfig.TIER2_STRESS_IMPACT.getAsDouble(),
                    FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.getAsInt(),
                    FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.getAsInt(),
                    FrontierProtocolServerConfig.TIER2_GRACE_PERIOD_TICKS.getAsInt(),
                    new CleanupSourceProfile(
                            FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.getAsInt(),
                            FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.getAsInt(),
                            FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.getAsInt()));
            case TIER_3 -> new StabilizerTierDefinition(
                    tier,
                    FrontierProtocolServerConfig.TIER3_CHUNK_RADIUS.getAsInt(),
                    FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.getAsInt(),
                    FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.getAsDouble(),
                    FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.getAsInt(),
                    FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.getAsInt(),
                    FrontierProtocolServerConfig.TIER3_GRACE_PERIOD_TICKS.getAsInt(),
                    new CleanupSourceProfile(
                            FrontierProtocolServerConfig.TIER3_CLEANUP_INTERVAL_TICKS.getAsInt(),
                            FrontierProtocolServerConfig.TIER3_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.getAsInt(),
                            FrontierProtocolServerConfig.TIER3_CLEANUP_MUTATION_BUDGET_PER_CYCLE.getAsInt()));
        };
    }
}
