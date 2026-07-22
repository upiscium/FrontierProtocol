package dev.upiscium.frontierprotocol.mixin;

import com.Harbinger.Spore.Sentities.Organoids.HiveTumor;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import dev.upiscium.frontierprotocol.compat.spore.SporeSuppressionQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {HiveTumor.class, Proto.class}, remap = false)
abstract class SporeFoliageOverrideSuppressionMixin {
    @Inject(method = "SpreadFoliageAndConvert", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void frontierProtocol$suppressOverrideTarget(
            Level level, BlockState state, BlockPos target, CallbackInfo callback) {
        if (SporeSuppressionQueries.isSuppressed(level, target)) {
            callback.cancel();
        }
    }
}
