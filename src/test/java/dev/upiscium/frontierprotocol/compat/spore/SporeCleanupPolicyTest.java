package dev.upiscium.frontierprotocol.compat.spore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class SporeCleanupPolicyTest {
    @Test
    void untaggedBlockIsKept() {
        assertTrue(SporeCleanupPolicy.replacementFor(Blocks.STONE.defaultBlockState(), false, false)
                .isEmpty());
    }

    @Test
    void removableBlockIsReplacedWithAir() {
        assertEquals(
                Blocks.AIR.defaultBlockState(),
                SporeCleanupPolicy.replacementFor(Blocks.SHORT_GRASS.defaultBlockState(), false, true)
                        .orElseThrow());
    }

    @Test
    void waterloggedRemovableBlockIsReplacedWithWater() {
        BlockState waterlogged = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true);
        assertEquals(
                Blocks.WATER.defaultBlockState(),
                SporeCleanupPolicy.replacementFor(waterlogged, false, true).orElseThrow());
    }

    @Test
    void neverTagTakesPriorityOverRemovableTag() {
        assertTrue(SporeCleanupPolicy.replacementFor(Blocks.SHORT_GRASS.defaultBlockState(), true, true)
                .isEmpty());
    }

    @Test
    void blockEntityStateIsKeptEvenWhenRemovable() {
        BlockState blockEntityState = Blocks.CHEST.defaultBlockState();
        assertTrue(SporeCleanupPolicy.replacementFor(blockEntityState, false, true)
                .isEmpty());
    }
}
