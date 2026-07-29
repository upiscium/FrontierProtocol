package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class TierOneStabilizerBlockTest {
    @Test
    void placementFacesThePlacerAndKeepsAxisAligned() {
        for (Direction playerDirection : Direction.Plane.HORIZONTAL) {
            Direction facing = TierOneStabilizerBlock.facingForPlacement(playerDirection);
            assertEquals(playerDirection.getOpposite(), facing);
            assertAligned(TierOneStabilizerBlock.orient(block().defaultBlockState(), facing));
        }
    }

    @Test
    void onlyRearFaceAcceptsShafts() {
        TierOneStabilizerBlock block = block();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState state = TierOneStabilizerBlock.orient(block.defaultBlockState(), facing);
            for (Direction face : Direction.values()) {
                assertEquals(face == facing.getOpposite(), block.hasShaftTowards(null, null, state, face));
            }
        }
    }

    @Test
    void rotationAndMirroringKeepFacingAndAxisAligned() {
        TierOneStabilizerBlock block = block();
        BlockState north = TierOneStabilizerBlock.orient(block.defaultBlockState(), Direction.NORTH);
        assertAligned(block.rotate(north, Rotation.CLOCKWISE_90));
        assertAligned(block.rotate(north, Rotation.CLOCKWISE_180));
        assertAligned(block.rotate(north, Rotation.COUNTERCLOCKWISE_90));
        assertAligned(block.mirror(north, Mirror.LEFT_RIGHT));
        assertAligned(block.mirror(north, Mirror.FRONT_BACK));
    }

    @Test
    void statusChangesPreserveOrientation() {
        BlockState state = TierOneStabilizerBlock.orient(block().defaultBlockState(), Direction.WEST);
        for (StabilizerStatus status : StabilizerStatus.values()) {
            BlockState changed = state.setValue(StabilizerBlock.STATUS, status);
            assertEquals(Direction.WEST, changed.getValue(TierOneStabilizerBlock.FACING));
            assertEquals(Direction.Axis.X, changed.getValue(StabilizerBlock.HORIZONTAL_AXIS));
        }
    }

    private static TierOneStabilizerBlock block() {
        assertTrue(ModBlocks.TIER_1_STABILIZER.isBound());
        assertFalse(ModBlocks.TIER_2_STABILIZER.get().defaultBlockState().hasProperty(TierOneStabilizerBlock.FACING));
        assertFalse(ModBlocks.TIER_3_STABILIZER.get().defaultBlockState().hasProperty(TierOneStabilizerBlock.FACING));
        return ModBlocks.TIER_1_STABILIZER.get();
    }

    private static void assertAligned(BlockState state) {
        assertEquals(
                state.getValue(TierOneStabilizerBlock.FACING).getAxis(),
                state.getValue(StabilizerBlock.HORIZONTAL_AXIS));
    }
}
