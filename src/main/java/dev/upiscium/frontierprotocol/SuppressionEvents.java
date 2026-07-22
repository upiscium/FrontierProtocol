package dev.upiscium.frontierprotocol;

import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionManager;
import dev.upiscium.frontierprotocol.ore.InitialSpawnOreSuppressionManager;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionSavedData;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class SuppressionEvents {
    private SuppressionEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        SpawnProtectionManager.rebuild(event.getServer().overworld());
        InitialSpawnOreSuppressionManager.rebuildSnapshot(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void levelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == net.minecraft.world.level.Level.OVERWORLD
                && level.getServer().getWorldData().overworldData().isInitialized()) {
            SpawnProtectionSavedData data = SpawnProtectionSavedData.get(level);
            data.initialize(new net.minecraft.world.level.ChunkPos(level.getSharedSpawnPos()));
            InitialSpawnOreSuppressionManager.rebuildSnapshot(level);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        InitialSpawnOreSuppressionManager.clear(event.getServer());
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ServerInfectionSuppressionService.INSTANCE.clear(level);
        }
    }
}
