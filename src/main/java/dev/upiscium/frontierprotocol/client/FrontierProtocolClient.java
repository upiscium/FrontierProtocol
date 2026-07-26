package dev.upiscium.frontierprotocol.client;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolClientConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;

@Mod(value = FrontierProtocolMod.MOD_ID, dist = Dist.CLIENT)
public final class FrontierProtocolClient {
    public FrontierProtocolClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FrontierProtocolClientConfig.SPEC);
        NeoForge.EVENT_BUS.register(FrontierProtocolClientEvents.class);
    }
}
