package dev.upiscium.frontierprotocol.mixin;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Mound.class, remap = false)
public interface SporeMoundInvoker {
    @Invoker(value = "placeStructureBlock", remap = false)
    void frontierProtocol$invokePlaceStructureBlock(Level level, BlockPos target);
}
