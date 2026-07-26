package dev.upiscium.frontierprotocol.config;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerLimits;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class FrontierProtocolServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SPAWN_PROTECTION_ENABLED = BUILDER
            .comment("Enable permanent infection suppression around the initial world spawn.")
            .define("spawnProtectionEnabled", true);
    public static final ModConfigSpec.IntValue SPAWN_PROTECTION_RADIUS_CHUNKS = BUILDER
            .comment("Initial spawn suppression radius in chunks. A radius of 2 covers 5x5 chunks.")
            .defineInRange("spawnProtectionRadiusChunks", 2, 0, 16);
    public static final ModConfigSpec.BooleanValue INITIAL_SPAWN_ORE_SUPPRESSION_ENABLED = BUILDER
            .comment("Suppress new standard ore feature placement around the initial world spawn.")
            .define("initialSpawnOreSuppressionEnabled", true);
    public static final ModConfigSpec.IntValue INITIAL_SPAWN_ORE_SUPPRESSION_RADIUS_CHUNKS = BUILDER
            .comment("Initial spawn ore suppression radius in chunks. A radius of 2 covers 5x5 chunks.")
            .defineInRange("initialSpawnOreSuppressionRadiusChunks", 2, 0, 16);
    public static final ModConfigSpec.IntValue TIER1_CHUNK_RADIUS = BUILDER
            .comment("Tier 1 Stabilizer coverage radius in chunks.")
            .defineInRange(
                    "tier1ChunkRadius",
                    0,
                    StabilizerLimits.MIN_CHUNK_RADIUS,
                    StabilizerLimits.MAX_CHUNK_RADIUS);
    public static final ModConfigSpec.IntValue TIER1_MINIMUM_RPM = BUILDER
            .comment("Minimum absolute rotational speed required by the Tier 1 Stabilizer.")
            .defineInRange("tier1MinimumRpm", 32, 1, 256);
    public static final ModConfigSpec.DoubleValue TIER1_STRESS_IMPACT = BUILDER
            .comment("Create stress impact of the Tier 1 Stabilizer.")
            .defineInRange("tier1StressImpact", 16.0, 0.0, 1024.0);
    public static final ModConfigSpec.IntValue TIER1_CELL_CAPACITY = BUILDER
            .comment("Maximum Stabilization Cells held by the Tier 1 Stabilizer.")
            .defineInRange(
                    "tier1CellCapacity",
                    8,
                    StabilizerLimits.MIN_CELL_CAPACITY,
                    StabilizerLimits.MAX_CELL_CAPACITY);
    public static final ModConfigSpec.IntValue TIER1_CELL_DURATION_TICKS = BUILDER
            .comment("Active ticks supplied by one Tier 1 Stabilization Cell.")
            .defineInRange("tier1CellDurationTicks", 6000, 1, 72000);
    public static final ModConfigSpec.IntValue TIER1_GRACE_PERIOD_TICKS = BUILDER
            .comment("Suppression time retained after the Tier 1 Stabilizer loses power or Cells.")
            .defineInRange("tier1GracePeriodTicks", 6000, 0, 72000);
    public static final ModConfigSpec.IntValue TIER1_CLEANUP_INTERVAL_TICKS = BUILDER
            .comment("Ticks between cleanup budget refreshes for each Tier 1 source.")
            .defineInRange("tier1CleanupIntervalTicks", 20, 1, 1200);
    public static final ModConfigSpec.IntValue TIER1_CLEANUP_INSPECTION_BUDGET_PER_CYCLE = BUILDER
            .comment("Maximum inspections sponsored by one Tier 1 source per cleanup cycle.")
            .defineInRange("tier1CleanupInspectionBudgetPerCycle", 128, 1, 8192);
    public static final ModConfigSpec.IntValue TIER1_CLEANUP_MUTATION_BUDGET_PER_CYCLE = BUILDER
            .comment("Maximum replacements sponsored by one Tier 1 source per cleanup cycle.")
            .defineInRange("tier1CleanupMutationBudgetPerCycle", 4, 1, 512);
    public static final ModConfigSpec.IntValue TIER2_CHUNK_RADIUS = BUILDER
            .comment("Tier 2 Stabilizer coverage radius in chunks.")
            .defineInRange(
                    "tier2ChunkRadius",
                    1,
                    StabilizerLimits.MIN_CHUNK_RADIUS,
                    StabilizerLimits.MAX_CHUNK_RADIUS);
    public static final ModConfigSpec.IntValue TIER2_MINIMUM_RPM = BUILDER
            .comment("Minimum absolute rotational speed required by the Tier 2 Stabilizer.")
            .defineInRange("tier2MinimumRpm", 64, 1, 256);
    public static final ModConfigSpec.DoubleValue TIER2_STRESS_IMPACT = BUILDER
            .comment("Create stress impact of the Tier 2 Stabilizer.")
            .defineInRange("tier2StressImpact", 64.0, 0.0, 1024.0);
    public static final ModConfigSpec.IntValue TIER2_CELL_CAPACITY = BUILDER
            .comment("Maximum Stabilization Cells held by the Tier 2 Stabilizer.")
            .defineInRange(
                    "tier2CellCapacity",
                    32,
                    StabilizerLimits.MIN_CELL_CAPACITY,
                    StabilizerLimits.MAX_CELL_CAPACITY);
    public static final ModConfigSpec.IntValue TIER2_CELL_DURATION_TICKS = BUILDER
            .comment("Active ticks supplied by one Tier 2 Stabilization Cell.")
            .defineInRange("tier2CellDurationTicks", 3000, 1, 72000);
    public static final ModConfigSpec.IntValue TIER2_GRACE_PERIOD_TICKS = BUILDER
            .comment("Suppression time retained after the Tier 2 Stabilizer loses power or Cells.")
            .defineInRange("tier2GracePeriodTicks", 9000, 0, 72000);
    public static final ModConfigSpec.IntValue TIER2_CLEANUP_INTERVAL_TICKS = BUILDER
            .comment("Ticks between cleanup budget refreshes for each Tier 2 source.")
            .defineInRange("tier2CleanupIntervalTicks", 20, 1, 1200);
    public static final ModConfigSpec.IntValue TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE = BUILDER
            .comment("Maximum inspections sponsored by one Tier 2 source per cleanup cycle.")
            .defineInRange("tier2CleanupInspectionBudgetPerCycle", 2048, 1, 8192);
    public static final ModConfigSpec.IntValue TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE = BUILDER
            .comment("Maximum replacements sponsored by one Tier 2 source per cleanup cycle.")
            .defineInRange("tier2CleanupMutationBudgetPerCycle", 64, 1, 512);
    public static final ModConfigSpec.IntValue TIER3_CHUNK_RADIUS = BUILDER
            .comment("Tier 3 Stabilizer coverage radius in chunks.")
            .defineInRange(
                    "tier3ChunkRadius",
                    2,
                    StabilizerLimits.MIN_CHUNK_RADIUS,
                    StabilizerLimits.MAX_CHUNK_RADIUS);
    public static final ModConfigSpec.IntValue TIER3_MINIMUM_RPM = BUILDER
            .comment("Minimum absolute rotational speed required by the Tier 3 Stabilizer.")
            .defineInRange("tier3MinimumRpm", 128, 1, 256);
    public static final ModConfigSpec.DoubleValue TIER3_STRESS_IMPACT = BUILDER
            .comment("Create stress impact of the Tier 3 Stabilizer.")
            .defineInRange("tier3StressImpact", 256.0, 0.0, 1024.0);
    public static final ModConfigSpec.IntValue TIER3_CELL_CAPACITY = BUILDER
            .comment("Maximum Stabilization Cells held by the Tier 3 Stabilizer.")
            .defineInRange(
                    "tier3CellCapacity",
                    64,
                    StabilizerLimits.MIN_CELL_CAPACITY,
                    StabilizerLimits.MAX_CELL_CAPACITY);
    public static final ModConfigSpec.IntValue TIER3_CELL_DURATION_TICKS = BUILDER
            .comment("Active ticks supplied by one Tier 3 Stabilization Cell.")
            .defineInRange("tier3CellDurationTicks", 2000, 1, 72000);
    public static final ModConfigSpec.IntValue TIER3_GRACE_PERIOD_TICKS = BUILDER
            .comment("Suppression time retained after the Tier 3 Stabilizer loses power or Cells.")
            .defineInRange("tier3GracePeriodTicks", 12000, 0, 72000);
    public static final ModConfigSpec.IntValue TIER3_CLEANUP_INTERVAL_TICKS = BUILDER
            .comment("Ticks between cleanup budget refreshes for each Tier 3 source.")
            .defineInRange("tier3CleanupIntervalTicks", 20, 1, 1200);
    public static final ModConfigSpec.IntValue TIER3_CLEANUP_INSPECTION_BUDGET_PER_CYCLE = BUILDER
            .comment("Maximum inspections sponsored by one Tier 3 source per cleanup cycle.")
            .defineInRange("tier3CleanupInspectionBudgetPerCycle", 8192, 1, 8192);
    public static final ModConfigSpec.IntValue TIER3_CLEANUP_MUTATION_BUDGET_PER_CYCLE = BUILDER
            .comment("Maximum replacements sponsored by one Tier 3 source per cleanup cycle.")
            .defineInRange("tier3CleanupMutationBudgetPerCycle", 256, 1, 512);
    public static final ModConfigSpec.BooleanValue PROGRESSIVE_CLEANUP_ENABLED = BUILDER
            .comment("Enable budgeted cleanup of audited removable infection foliage.")
            .define("progressiveCleanupEnabled", true);
    public static final ModConfigSpec.IntValue CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK = BUILDER
            .comment("Maximum cleanup BlockState inspections across all dimensions per server tick.")
            .defineInRange("cleanupGlobalInspectionBudgetPerTick", 512, 1, 65536);
    public static final ModConfigSpec.IntValue CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK = BUILDER
            .comment("Maximum cleanup block replacements across all dimensions per server tick.")
            .defineInRange("cleanupGlobalMutationBudgetPerTick", 16, 1, 4096);
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER.define("debugLogging", false);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private FrontierProtocolServerConfig() {}
}
