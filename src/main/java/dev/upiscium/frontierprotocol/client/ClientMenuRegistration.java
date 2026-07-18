package dev.upiscium.frontierprotocol.client;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientMenuRegistration {
    private ClientMenuRegistration() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RESOURCE_NODE.get(), ResourceNodeScreen::new);
        event.register(ModMenus.OIL_WELL.get(), OilWellScreen::new);
    }
}
