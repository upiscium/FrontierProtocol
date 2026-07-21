package dev.upiscium.frontierprotocol.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FrontierProtocolServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SPAWN_PROTECTION_ENABLED = BUILDER
            .comment("Enable permanent infection suppression around the initial world spawn.")
            .define("spawnProtectionEnabled", true);
    public static final ModConfigSpec.IntValue SPAWN_PROTECTION_RADIUS_CHUNKS = BUILDER
            .comment("Initial spawn suppression radius in chunks. A radius of 2 covers 5x5 chunks.")
            .defineInRange("spawnProtectionRadiusChunks", 2, 0, 16);
    public static final ModConfigSpec.IntValue TIER1_MINIMUM_RPM = BUILDER
            .comment("Minimum absolute rotational speed required by the Tier 1 Stabilizer.")
            .defineInRange("tier1MinimumRpm", 32, 1, 256);
    public static final ModConfigSpec.DoubleValue TIER1_STRESS_IMPACT = BUILDER
            .comment("Create stress impact of the Tier 1 Stabilizer.")
            .defineInRange("tier1StressImpact", 16.0, 0.0, 1024.0);
    public static final ModConfigSpec.IntValue TIER1_GRACE_PERIOD_TICKS = BUILDER
            .comment("Suppression time retained after the Tier 1 Stabilizer loses power or consumables.")
            .defineInRange("tier1GracePeriodTicks", 6000, 0, 72000);
    public static final ModConfigSpec.IntValue TIER1_CONSUMABLE_DURATION_TICKS = BUILDER
            .comment("Active ticks supplied by one Tier 1 stabilization compound.")
            .defineInRange("tier1ConsumableDurationTicks", 6000, 1, 72000);
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER.define("debugLogging", false);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private FrontierProtocolServerConfig() {}
}
