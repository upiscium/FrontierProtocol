package dev.upiscium.frontierprotocol.tier1;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class Tier1StabilizerBlock extends HorizontalAxisKineticBlock implements IBE<Tier1StabilizerBlockEntity> {
    public static final MapCodec<Tier1StabilizerBlock> CODEC = simpleCodec(Tier1StabilizerBlock::new);
    public static final EnumProperty<Tier1StabilizerStatus> STATUS =
            EnumProperty.create("status", Tier1StabilizerStatus.class);

    public Tier1StabilizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(STATUS, Tier1StabilizerStatus.OFFLINE));
    }

    @Override
    protected MapCodec<? extends HorizontalAxisKineticBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATUS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_AXIS);
    }

    @Override
    public Class<Tier1StabilizerBlockEntity> getBlockEntityClass() {
        return Tier1StabilizerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends Tier1StabilizerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.TIER_1_STABILIZER.get();
    }
}
