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
import dev.upiscium.frontierprotocol.client.render.StabilizerBlockEntityRenderer;
import dev.upiscium.frontierprotocol.client.render.StabilizerRenderModels;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = FrontierProtocolMod.MOD_ID, dist = Dist.CLIENT)
public final class FrontierProtocolClient {
    public FrontierProtocolClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FrontierProtocolClientConfig.SPEC);
        NeoForge.EVENT_BUS.register(FrontierProtocolClientEvents.class);
        modBus.addListener(FrontierProtocolClient::clientSetup);
        modBus.addListener(FrontierProtocolClient::registerRenderers);
        modBus.addListener(FrontierProtocolClient::registerLayerDefinitions);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new FrontierProtocolPonderPlugin());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.STABILIZER.get(), StabilizerBlockEntityRenderer::new);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StabilizerRenderModels.TIER_1, StabilizerRenderModels::createTierOneLayer);
    }
}
