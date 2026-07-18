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
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER.define("debugLogging", false);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private FrontierProtocolServerConfig() {}
}
