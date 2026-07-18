package dev.upiscium.frontierprotocol;

import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionManager;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class SuppressionEvents {
    private SuppressionEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        SpawnProtectionManager.rebuild(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ServerInfectionSuppressionService.INSTANCE.clear(level);
        }
    }
}
