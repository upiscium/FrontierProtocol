package dev.upiscium.frontierprotocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

class SuppressionConfigEventsTest {
    @Test
    void acceptsOnlyFrontierProtocolServerConfig() {
        assertTrue(SuppressionConfigEvents.isTargetServerConfig(
                ModConfig.Type.SERVER, FrontierProtocolServerConfig.SPEC));
        assertFalse(SuppressionConfigEvents.isTargetServerConfig(
                ModConfig.Type.COMMON, FrontierProtocolServerConfig.SPEC));
        assertFalse(SuppressionConfigEvents.isTargetServerConfig(
                ModConfig.Type.SERVER, new ModConfigSpec.Builder().build()));
    }
}
