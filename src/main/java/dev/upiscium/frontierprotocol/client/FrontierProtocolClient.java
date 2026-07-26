package dev.upiscium.frontierprotocol.client;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolClientConfig;
import dev.upiscium.frontierprotocol.client.ponder.FrontierProtocolPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = FrontierProtocolMod.MOD_ID, dist = Dist.CLIENT)
public final class FrontierProtocolClient {
    public FrontierProtocolClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FrontierProtocolClientConfig.SPEC);
        NeoForge.EVENT_BUS.register(FrontierProtocolClientEvents.class);
        modBus.addListener(FrontierProtocolClient::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new FrontierProtocolPonderPlugin());
    }
}
