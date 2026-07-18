package dev.upiscium.frontierprotocol.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class FrontierProtocolServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SECTOR_SIZE_CHUNKS = BUILDER
            .comment("Sector side length in chunks. Stored per world after initialization.")
            .defineInRange("sectorSizeChunks", 8, 1, 128);
    public static final ModConfigSpec.IntValue PLACEMENT_VERSION = BUILDER
            .comment("Deterministic placement algorithm version for newly initialized worlds.")
            .defineInRange("placementVersion", 1, 1, 1);
    public static final ModConfigSpec.IntValue LOCATE_RADIUS = BUILDER
            .comment("Maximum sector radius searched by the locate command.")
            .defineInRange("locateRadius", 128, 1, 128);
    public static final ModConfigSpec.IntValue INITIAL_PROTECTION_RADIUS = BUILDER
            .comment("Permanent protection radius around the initial spawn chunk.")
            .defineInRange("initialProtectionRadius", 2, 0, 16);
    public static final ModConfigSpec.IntValue BEACON_RADIUS = BUILDER
            .comment("Stabilization Beacon protection radius in chunks.")
            .defineInRange("beaconRadius", 1, 0, 8);
    public static final ModConfigSpec.IntValue BEACON_GRACE_TICKS = BUILDER
            .comment("Protection grace duration after a beacon exhausts its fuel.")
            .defineInRange("beaconGraceTicks", 6000, 0, 20 * 60 * 60);

    public static final ModConfigSpec.BooleanValue SCALE_NATURAL_SPAWNS = BUILDER
            .define("mobScaling.scaleNaturalSpawns", true);
    public static final ModConfigSpec.BooleanValue SCALE_CHUNK_GENERATION_SPAWNS = BUILDER
            .define("mobScaling.scaleChunkGenerationSpawns", true);
    public static final ModConfigSpec.BooleanValue SCALE_SPAWNER_SPAWNS = BUILDER
            .define("mobScaling.scaleSpawnerSpawns", true);
    public static final ModConfigSpec.BooleanValue SCALE_NEST_SPAWNS = BUILDER
            .define("mobScaling.scaleFrontierProtocolNestSpawns", true);
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MOB_TIER_DISTANCES = BUILDER
            .comment("Inclusive Chebyshev chunk distance where each mob scaling tier begins.")
            .defineList("mobScaling.tierDistances", List.of(0, 16, 40, 80, 128),
                    value -> value instanceof Integer integer && integer >= 0);
    public static final ModConfigSpec.ConfigValue<? extends List<? extends Number>> MOB_HEALTH_MULTIPLIERS = BUILDER
            .defineList("mobScaling.healthMultipliers", List.of(1.0, 1.25, 1.60, 2.10, 2.70),
                    FrontierProtocolServerConfig::positiveNumber);
    public static final ModConfigSpec.ConfigValue<? extends List<? extends Number>> MOB_ATTACK_MULTIPLIERS = BUILDER
            .defineList("mobScaling.attackMultipliers", List.of(1.0, 1.15, 1.35, 1.60, 1.90),
                    FrontierProtocolServerConfig::positiveNumber);
    public static final ModConfigSpec.ConfigValue<? extends List<? extends Number>> MOB_ARMOR_ADDITIONS = BUILDER
            .defineList("mobScaling.armorAdditions", List.of(0.0, 1.0, 3.0, 5.0, 8.0),
                    FrontierProtocolServerConfig::nonNegativeNumber);
    public static final ModConfigSpec.ConfigValue<? extends List<? extends Number>> MOB_SPEED_MULTIPLIERS = BUILDER
            .defineList("mobScaling.speedMultipliers", List.of(1.0, 1.02, 1.05, 1.08, 1.12),
                    FrontierProtocolServerConfig::positiveNumber);

    public static final ModConfigSpec.IntValue INFECTION_SLOW_TICK_INTERVAL = BUILDER
            .defineInRange("infection.slowTickInterval", 100, 1, 1200);
    public static final ModConfigSpec.IntValue INFECTION_CHUNK_BUDGET = BUILDER
            .defineInRange("infection.chunkBudget", 32, 1, 4096);
    public static final ModConfigSpec.IntValue INFECTION_PRESSURE_PER_CARRIER = BUILDER
            .defineInRange("infection.pressurePerCarrier", 1, 0, 1000);
    public static final ModConfigSpec.IntValue INFECTION_CARRIER_KILL_REDUCTION = BUILDER
            .defineInRange("infection.carrierKillReduction", 2, 0, 1000);
    public static final ModConfigSpec.IntValue INFECTION_MAX_PRESSURE = BUILDER
            .defineInRange("infection.maxPressure", 100, 1, 100000);
    public static final ModConfigSpec.IntValue INFECTION_CORE_THRESHOLD = BUILDER
            .defineInRange("infection.coreThreshold", 60, 1, 100000);
    public static final ModConfigSpec.IntValue INFECTION_CORE_MATURATION_TICKS = BUILDER
            .defineInRange("infection.coreMaturationTicks", 24000, 1, 20 * 60 * 60 * 24);
    public static final ModConfigSpec.IntValue INFECTION_CORE_BREAK_REDUCTION = BUILDER
            .defineInRange("infection.coreBreakPressureReduction", 30, 0, 100000);
    public static final ModConfigSpec.IntValue INFECTION_NEST_BREAK_REDUCTION = BUILDER
            .defineInRange("infection.nestBreakPressureReduction", 50, 0, 100000);
    public static final ModConfigSpec.IntValue INFECTION_CORE_CANDIDATES = BUILDER
            .defineInRange("infection.corePlacementCandidates", 8, 1, 64);
    public static final ModConfigSpec.IntValue INFECTION_NEST_MIN_SPAWN_INTERVAL = BUILDER
            .defineInRange("infection.nestMinSpawnInterval", 600, 1, 72000);
    public static final ModConfigSpec.IntValue INFECTION_NEST_MAX_SPAWN_INTERVAL = BUILDER
            .defineInRange("infection.nestMaxSpawnInterval", 1200, 1, 72000);
    public static final ModConfigSpec.IntValue INFECTION_NEST_LOCAL_CAP = BUILDER
            .defineInRange("infection.nestLocalCap", 12, 0, 256);
    public static final ModConfigSpec.IntValue INFECTION_NEST_LOCAL_RADIUS = BUILDER
            .defineInRange("infection.nestLocalRadius", 24, 1, 128);
    public static final ModConfigSpec.IntValue INFECTION_NEST_GLOBAL_CAP = BUILDER
            .defineInRange("infection.nestGlobalCap", 80, 0, 4096);
    public static final ModConfigSpec.IntValue INFECTION_PLAYER_ACTIVITY_RADIUS = BUILDER
            .defineInRange("infection.playerActivityRadius", 128, 1, 512);

    public static final ModConfigSpec.IntValue BREACH_NO_PATH_TICKS = BUILDER
            .defineInRange("breach.noPathTicks", 40, 1, 1200);
    public static final ModConfigSpec.DoubleValue BREACH_MAX_HARDNESS = BUILDER
            .defineInRange("breach.maxHardness", 2.0, 0.0, 50.0);
    public static final ModConfigSpec.DoubleValue BREACH_REACH = BUILDER
            .defineInRange("breach.reach", 2.5, 1.0, 6.0);
    public static final ModConfigSpec.DoubleValue BREACH_TIME_MULTIPLIER = BUILDER
            .defineInRange("breach.timeMultiplier", 1.0, 0.05, 100.0);
    public static final ModConfigSpec.IntValue BREACH_MAX_CANDIDATES = BUILDER
            .defineInRange("breach.maxCandidates", 12, 1, 32);
    public static final ModConfigSpec.BooleanValue BREACH_DROPS = BUILDER
            .define("breach.dropBlocks", false);

    public static final ModConfigSpec.IntValue NUTRITION_HISTORY_LENGTH = BUILDER
            .defineInRange("nutrition.historyLength", 8, 1, 64);
    public static final ModConfigSpec.ConfigValue<? extends List<? extends Number>> NUTRITION_ITEM_EFFICIENCIES = BUILDER
            .defineList("nutrition.itemEfficiencies", List.of(1.0, 0.85, 0.65, 0.45, 0.25),
                    value -> value instanceof Number number && Double.isFinite(number.doubleValue())
                            && number.doubleValue() >= 0.25 && number.doubleValue() <= 1.0);
    public static final ModConfigSpec.IntValue NUTRITION_CATEGORY_REPEAT_THRESHOLD = BUILDER
            .defineInRange("nutrition.categoryRepeatThreshold", 6, 1, 64);
    public static final ModConfigSpec.DoubleValue NUTRITION_CATEGORY_MULTIPLIER = BUILDER
            .defineInRange("nutrition.categoryMultiplier", 0.8, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue NUTRITION_MINIMUM_EFFICIENCY = BUILDER
            .defineInRange("nutrition.minimumEfficiency", 0.25, 0.25, 1.0);
    public static final ModConfigSpec.BooleanValue NUTRITION_RECORD_CREATIVE = BUILDER
            .define("nutrition.recordCreativeMeals", true);

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER.define("debugLogging", false);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private FrontierProtocolServerConfig() {}

    private static boolean positiveNumber(Object value) {
        return value instanceof Number number && Double.isFinite(number.doubleValue()) && number.doubleValue() > 0;
    }

    private static boolean nonNegativeNumber(Object value) {
        return value instanceof Number number && Double.isFinite(number.doubleValue()) && number.doubleValue() >= 0;
    }
}
