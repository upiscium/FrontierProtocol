package dev.upiscium.frontierprotocol.mixin;

import dev.upiscium.frontierprotocol.ore.InitialSpawnOreSuppressionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract class InitialSpawnOreSnapshotMixin {
    // setSpawn ordinals: 0 is the debug spawn, 1 is the normal initial candidate, and 2 is the safe final spawn.
    @Redirect(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/ServerLevelData;setSpawn(Lnet/minecraft/core/BlockPos;F)V",
                    ordinal = 1),
            require = 1)
    private static void frontierProtocol$publishProvisionalSnapshot(
            ServerLevelData data,
            BlockPos spawn,
            float angle,
            ServerLevel level,
            ServerLevelData methodData,
            boolean generateBonusChest,
            boolean debug) {
        InitialSpawnOreSuppressionManager.publishProvisional(level, new ChunkPos(spawn), 5);
        data.setSpawn(spawn, angle);
    }

    @Inject(method = "setInitialSpawn", at = @At("RETURN"), require = 1)
    private static void frontierProtocol$publishFinalSnapshot(
            ServerLevel level,
            ServerLevelData levelData,
            boolean generateBonusChest,
            boolean debug,
            CallbackInfo callback) {
        InitialSpawnOreSuppressionManager.initializeFinalSnapshot(level, new ChunkPos(levelData.getSpawnPos()));
    }
}
