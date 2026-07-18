package dev.upiscium.frontierprotocol.protection;

import com.mojang.serialization.MapCodec;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.Containers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class StabilizationBeaconBlock extends BaseEntityBlock {
    public static final MapCodec<StabilizationBeaconBlock> CODEC = simpleCodec(StabilizationBeaconBlock::new);
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public StabilizationBeaconBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ENABLED, true));
    }

    @Override
    protected MapCodec<StabilizationBeaconBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StabilizationBeaconBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type, ModBlockEntities.STABILIZATION_BEACON.get(), StabilizationBeaconBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ENABLED,
                !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        boolean enabled = !level.hasNeighborSignal(pos);
        if (enabled != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, enabled), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel
                && level.getBlockEntity(pos) instanceof StabilizationBeaconBlockEntity beacon) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), beacon.inventory().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
