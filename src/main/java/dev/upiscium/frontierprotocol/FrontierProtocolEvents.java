package dev.upiscium.frontierprotocol;

import dev.upiscium.frontierprotocol.command.FrontierProtocolCommands;
import dev.upiscium.frontierprotocol.data.TraitReloadListener;
import dev.upiscium.frontierprotocol.data.ResourceNodeReloadListener;
import dev.upiscium.frontierprotocol.data.OilWellReloadListener;
import dev.upiscium.frontierprotocol.sector.SectorDiscoveryService;
import dev.upiscium.frontierprotocol.sector.SectorGuaranteeService;
import dev.upiscium.frontierprotocol.protection.ProtectionIndex;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.server.level.ServerLevel;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class FrontierProtocolEvents {
    private FrontierProtocolEvents() {}

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new TraitReloadListener());
        event.addListener(new ResourceNodeReloadListener());
        event.addListener(new OilWellReloadListener());
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        var overworld = event.getServer().overworld();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(overworld);
        data.initialize(overworld);
        SectorGuaranteeService.initialize(overworld, data);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        FrontierProtocolCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void enteringSection(EntityEvent.EnteringSection event) {
        if (event.didChunkChange() && event.getEntity() instanceof ServerPlayer player) {
            SectorDiscoveryService.discoverCurrentSector(player);
        }
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SectorDiscoveryService.discoverCurrentSector(player);
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ProtectionIndex.clear(level);
    }
}
