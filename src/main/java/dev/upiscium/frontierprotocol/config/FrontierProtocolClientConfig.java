package dev.upiscium.frontierprotocol.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FrontierProtocolClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_STABILIZER_GOGGLE_DETAILS = BUILDER
            .comment("Show live Stabilizer diagnostics in Engineer's Goggles.")
            .define("showStabilizerGoggleDetails", true);
    public static final ModConfigSpec.BooleanValue SHOW_STABILIZER_RANGE_OVERLAY = BUILDER
            .comment("Show the targeted Stabilizer chunk range while wearing Engineer's Goggles.")
            .define("showStabilizerRangeOverlay", true);
    public static final ModConfigSpec.BooleanValue RANGE_OVERLAY_REQUIRES_SNEAKING = BUILDER
            .comment("Require the sneak key to be held before showing a Stabilizer range.")
            .define("rangeOverlayRequiresSneaking", true);
    public static final ModConfigSpec.BooleanValue SHOW_RANGE_VERTICAL_CORNERS = BUILDER
            .comment("Draw vertical corner lines on the Stabilizer range overlay.")
            .define("showRangeVerticalCorners", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FrontierProtocolClientConfig() {}
}
