package dev.upiscium.frontierprotocol;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.ore.InitialSpawnOreSuppressionManager;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

final class SuppressionConfigEvents {
    private SuppressionConfigEvents() {}

    static void configReloading(ModConfigEvent.Reloading event) {
        ModConfig config = event.getConfig();
        if (!isTargetServerConfig(config.getType(), config.getSpec())) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) queueRebuild(server);
    }

    static boolean isTargetServerConfig(ModConfig.Type type, Object spec) {
        return type == ModConfig.Type.SERVER && spec == FrontierProtocolServerConfig.SPEC;
    }

    static void queueRebuild(MinecraftServer server) {
        server.execute(() -> {
            SpawnProtectionManager.rebuild(server.overworld());
            InitialSpawnOreSuppressionManager.rebuildSnapshot(server.overworld());
        });
    }
}
