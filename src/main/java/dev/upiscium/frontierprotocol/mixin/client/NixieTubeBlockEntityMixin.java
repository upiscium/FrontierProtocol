package dev.upiscium.frontierprotocol.mixin.client;

import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlock;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import dev.upiscium.frontierprotocol.client.nixie.StabilizerNixieSignalResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NixieTubeBlockEntity.class)
public abstract class NixieTubeBlockEntityMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void frontierProtocol$showStabilizerStatus(CallbackInfo callbackInfo) {
        NixieTubeBlockEntity nixie = (NixieTubeBlockEntity) (Object) this;
        Level level = nixie.getLevel();
        if (level == null || !level.isClientSide || nixie.computerBehaviour.hasAttachedComputer()) return;

        BlockState nixieState = nixie.getBlockState();
        Direction facing = NixieTubeBlock.getFacing(nixieState);
        BlockPos connectedPos = nixie.getBlockPos().relative(facing.getOpposite());
        StabilizerNixieSignalResolver.resolve(level.getBlockState(connectedPos))
                .ifPresent(signalState -> nixie.signalState = signalState);
    }
}
