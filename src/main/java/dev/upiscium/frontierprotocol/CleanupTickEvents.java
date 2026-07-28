package dev.upiscium.frontierprotocol;

import dev.upiscium.frontierprotocol.cleanup.ServerInfectionCleanupService;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class CleanupTickEvents {
    private CleanupTickEvents() {}

    @SubscribeEvent
    public static void serverTickPost(ServerTickEvent.Post event) {
        ServerInfectionCleanupService.INSTANCE.tick(event.getServer());
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level);
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        ServerInfectionCleanupService.INSTANCE.clearRuntime(event.getServer());
    }
}
