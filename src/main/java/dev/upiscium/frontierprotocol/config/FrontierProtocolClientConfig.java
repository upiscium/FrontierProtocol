package dev.upiscium.frontierprotocol.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FrontierProtocolClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SHOW_DISCOVERY_MESSAGES = BUILDER
            .comment("Show a message when a special sector is discovered.")
            .define("showDiscoveryMessages", true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private FrontierProtocolClientConfig() {}
}
