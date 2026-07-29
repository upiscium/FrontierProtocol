package dev.upiscium.frontierprotocol.stabilizer;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public final class TierOneStabilizerBlock extends StabilizerBlock {
    public static final MapCodec<TierOneStabilizerBlock> CODEC = simpleCodec(TierOneStabilizerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TierOneStabilizerBlock(Properties properties) {
        super(StabilizerTier.TIER_1, properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.EAST)
                .setValue(HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(STATUS, StabilizerStatus.OFFLINE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = facingForPlacement(context.getHorizontalDirection());
        return orient(defaultBlockState(), facing);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, net.minecraft.core.BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return orient(state, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    public static BlockState orient(BlockState state, Direction facing) {
        if (!facing.getAxis().isHorizontal()) throw new IllegalArgumentException("facing must be horizontal");
        return state.setValue(FACING, facing).setValue(HORIZONTAL_AXIS, facing.getAxis());
    }

    public static Direction facingForPlacement(Direction playerDirection) {
        if (!playerDirection.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("player direction must be horizontal");
        }
        return playerDirection.getOpposite();
    }

    @Override
    protected MapCodec<? extends HorizontalAxisKineticBlock> codec() {
        return CODEC;
    }
}
