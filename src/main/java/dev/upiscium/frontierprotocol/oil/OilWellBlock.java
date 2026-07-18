package dev.upiscium.frontierprotocol.oil;

import com.mojang.serialization.MapCodec;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class OilWellBlock extends BaseEntityBlock {
    public static final MapCodec<OilWellBlock> CODEC = simpleCodec(OilWellBlock::new);

    public OilWellBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<OilWellBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OilWellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type, ModBlockEntities.OIL_WELL.get(), OilWellBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OilWellBlockEntity well)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(well, well::writeMenuData);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
