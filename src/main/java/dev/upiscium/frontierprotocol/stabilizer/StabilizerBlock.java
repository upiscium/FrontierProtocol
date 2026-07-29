package dev.upiscium.frontierprotocol.stabilizer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;

public final class StabilizerBlock extends HorizontalAxisKineticBlock implements IBE<StabilizerBlockEntity> {
    public static final MapCodec<StabilizerBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    StringRepresentable.fromEnum(StabilizerTier::values)
                            .fieldOf("tier")
                            .forGetter(StabilizerBlock::tier),
                    propertiesCodec())
            .apply(instance, StabilizerBlock::new));
    public static final EnumProperty<StabilizerStatus> STATUS =
            EnumProperty.create("status", StabilizerStatus.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final StabilizerTier tier;

    public StabilizerBlock(StabilizerTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.EAST)
                .setValue(HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(STATUS, StabilizerStatus.OFFLINE));
    }

    public StabilizerTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends HorizontalAxisKineticBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, STATUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withFacing(defaultBlockState(), facingForPlacement(context.getHorizontalDirection()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return withFacing(state, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    static BlockState withFacing(BlockState state, Direction facing) {
        return state.setValue(FACING, facing).setValue(HORIZONTAL_AXIS, facing.getAxis());
    }

    static Direction facingForPlacement(Direction playerFacing) {
        return playerFacing.getOpposite();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof StabilizerBlockEntity blockEntity) {
            blockEntity.dropInventory(serverLevel);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<StabilizerBlockEntity> getBlockEntityClass() {
        return StabilizerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StabilizerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.STABILIZER.get();
    }
}
