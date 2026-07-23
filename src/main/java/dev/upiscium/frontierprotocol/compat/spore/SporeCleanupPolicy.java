package dev.upiscium.frontierprotocol.compat.spore;

import dev.upiscium.frontierprotocol.registry.ModBlockTags;
import java.util.Optional;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class SporeCleanupPolicy {
    private SporeCleanupPolicy() {}

    public static Optional<BlockState> replacementFor(BlockState state) {
        return replacementFor(
                state, state.is(ModBlockTags.CLEANUP_NEVER), state.is(ModBlockTags.CLEANUP_REMOVABLE));
    }

    static Optional<BlockState> replacementFor(BlockState state, boolean never, boolean removable) {
        if (state.hasBlockEntity() || never || !removable) {
            return Optional.empty();
        }
        return Optional.of(state.getFluidState().getType().isSame(Fluids.WATER)
                ? Blocks.WATER.defaultBlockState()
                : Blocks.AIR.defaultBlockState());
    }
}
