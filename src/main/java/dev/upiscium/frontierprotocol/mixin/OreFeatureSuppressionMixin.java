package dev.upiscium.frontierprotocol.mixin;

import dev.upiscium.frontierprotocol.api.ore.OreGenerationSuppressionApi;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OreFeature.class)
abstract class OreFeatureSuppressionMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, require = 1)
    private void frontierProtocol$suppressInitialSpawnOre(
            FeaturePlaceContext<OreConfiguration> context, CallbackInfoReturnable<Boolean> callback) {
        if (OreGenerationSuppressionApi.isSuppressed(context.level(), context.origin())) {
            callback.setReturnValue(false);
        }
    }
}
