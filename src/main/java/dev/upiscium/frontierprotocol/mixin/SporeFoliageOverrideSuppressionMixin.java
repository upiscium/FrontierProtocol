package dev.upiscium.frontierprotocol.mixin;

import com.Harbinger.Spore.Sblocks.CDUBlock;
import com.Harbinger.Spore.Sentities.Organoids.HiveTumor;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import dev.upiscium.frontierprotocol.compat.spore.SporeSuppressionQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {HiveTumor.class, Proto.class}, remap = false)
abstract class SporeFoliageOverrideSuppressionMixin {
    @Redirect(
            method = "SpreadFoliageAndConvert(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lcom/Harbinger/Spore/Sblocks/CDUBlock;replaceCDU(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;)V"),
            require = 1,
            remap = false)
    private void frontierProtocol$guardCduReplacement(BlockPos target, Level level) {
        if (!SporeSuppressionQueries.isSuppressed(level, target)) {
            CDUBlock.replaceCDU(target, level);
        }
    }
}
