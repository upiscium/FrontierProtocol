package dev.upiscium.frontierprotocol.mixin;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import dev.upiscium.frontierprotocol.compat.spore.SporeSuppressionQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mound.class, remap = false)
abstract class SporeMoundStructureSuppressionMixin {
    @Inject(method = "placeStructureBlock", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void frontierProtocol$suppressStructureTarget(Level level, BlockPos target, CallbackInfo callback) {
        if (SporeSuppressionQueries.isSuppressed(level, target)) {
            callback.cancel();
        }
    }
}
