package dev.upiscium.frontierprotocol.resource;

import com.mojang.serialization.MapCodec;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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

public final class ResourceNodeBlock extends BaseEntityBlock {
    public static final MapCodec<ResourceNodeBlock> CODEC = simpleCodec(ResourceNodeBlock::new);

    public ResourceNodeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ResourceNodeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type, ModBlockEntities.RESOURCE_NODE.get(), ResourceNodeBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ResourceNodeBlockEntity node)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(node, node::writeMenuData);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof ResourceNodeBlockEntity node) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), node.output().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
