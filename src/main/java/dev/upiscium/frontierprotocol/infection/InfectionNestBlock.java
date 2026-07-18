package dev.upiscium.frontierprotocol.infection;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class InfectionNestBlock extends BaseEntityBlock {
    public static final MapCodec<InfectionNestBlock> CODEC = simpleCodec(InfectionNestBlock::new);

    public InfectionNestBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<InfectionNestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfectionNestBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            InfectionService.onInfectionBlockDestroyed(serverLevel, pos, true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
