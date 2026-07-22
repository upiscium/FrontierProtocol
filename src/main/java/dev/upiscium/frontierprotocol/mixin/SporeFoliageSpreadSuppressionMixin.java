package dev.upiscium.frontierprotocol.mixin;

import com.Harbinger.Spore.Sentities.FoliageSpread;
import dev.upiscium.frontierprotocol.compat.spore.SporeSuppressionQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FoliageSpread.class, remap = false)
interface SporeFoliageSpreadSuppressionMixin {
    @Redirect(
            method = "placeGroundFoliage(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 1,
            remap = false)
    private boolean frontierProtocol$guardGroundFoliage(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "placeRottenBush(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 1,
            remap = false)
    private boolean frontierProtocol$guardRottenBush(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "placeWaterFoliage(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 2,
            remap = false)
    private boolean frontierProtocol$guardWaterFoliage(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "placeHangingFoliage(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 2,
            remap = false)
    private boolean frontierProtocol$guardHangingFoliage(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "placeWallFoliage(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;ZZZZLnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 4,
            remap = false)
    private boolean frontierProtocol$guardWallFoliage(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "placeBranches(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 4,
            remap = false)
    private boolean frontierProtocol$guardBranches(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "placeCropsFoliage(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 1,
            remap = false)
    private boolean frontierProtocol$guardCropFoliage(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "convertFromJson(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 1,
            remap = false)
    private boolean frontierProtocol$guardJsonConversion(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "convertBlocks(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 1,
            remap = false)
    private boolean frontierProtocol$guardConfiguredConversion(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "convertWood(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 4,
            remap = false)
    private boolean frontierProtocol$guardWoodConversion(
            Level level, BlockPos target, BlockState state, int flags) {
        return SporeSuppressionQueries.setBlock(level, target, state, flags);
    }

    @Redirect(
            method = "convertWood(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;fall(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/entity/item/FallingBlockEntity;"),
            require = 3,
            remap = false)
    private FallingBlockEntity frontierProtocol$guardFallingWoodConversion(
            Level level, BlockPos target, BlockState state) {
        return SporeSuppressionQueries.isSuppressed(level, target)
                ? null
                : FallingBlockEntity.fall(level, target, state);
    }
}
