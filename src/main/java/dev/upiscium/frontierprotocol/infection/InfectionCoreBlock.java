package dev.upiscium.frontierprotocol.infection;

import com.mojang.serialization.MapCodec;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class InfectionCoreBlock extends Block {
    public static final MapCodec<InfectionCoreBlock> CODEC = simpleCodec(InfectionCoreBlock::new);

    public InfectionCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && !newState.is(ModBlocks.INFECTION_NEST.get())
                && level instanceof ServerLevel serverLevel) {
            InfectionService.onInfectionBlockDestroyed(serverLevel, pos, false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
