package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.Test;

class StabilizerBlockTest {
    @Test
    void defaultsAndPlacementFaceThePlayerWithMatchingAxis() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerBlock block = block(tier);
            assertFacing(block.defaultBlockState(), Direction.EAST);
            for (Direction playerFacing : Direction.Plane.HORIZONTAL) {
                Direction facing = StabilizerBlock.facingForPlacement(playerFacing);
                assertEquals(playerFacing.getOpposite(), facing);
                assertFacing(StabilizerBlock.withFacing(block.defaultBlockState(), facing), facing);
            }
        }
    }

    @Test
    void onlyRearFaceExposesShaftForEveryTierAndFacing() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerBlock block = block(tier);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = StabilizerBlock.withFacing(block.defaultBlockState(), facing);
                for (Direction face : Direction.values()) {
                    assertEquals(
                            face == facing.getOpposite(),
                            block.hasShaftTowards(null, BlockPos.ZERO, state, face));
                }
            }
        }
    }

    @Test
    void rotationMirrorAndStatusChangesPreserveFacingAxisInvariant() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerBlock block = block(tier);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = StabilizerBlock.withFacing(block.defaultBlockState(), facing);
                for (Rotation rotation : Rotation.values()) {
                    BlockState rotated = block.rotate(state, rotation);
                    assertFacing(rotated, rotation.rotate(facing));
                }
                for (Mirror mirror : Mirror.values()) {
                    BlockState mirrored = block.mirror(state, mirror);
                    assertEquals(mirror.mirror(facing), mirrored.getValue(StabilizerBlock.FACING));
                    assertFacing(mirrored, mirrored.getValue(StabilizerBlock.FACING));
                }
                for (StabilizerStatus status : StabilizerStatus.values()) {
                    assertFacing(state.setValue(StabilizerBlock.STATUS, status), facing);
                }
            }
        }
    }

    @Test
    void codecRoundTripRetainsTierAndDirectionalContract() throws ReflectiveOperationException {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerBlock original = block(tier);
            JsonElement encoded = StabilizerBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            StabilizerBlock decoded = decode(StabilizerBlock.CODEC, encoded);

            assertEquals(tier, decoded.tier());
            assertTrue(decoded.defaultBlockState().hasProperty(StabilizerBlock.FACING));
            assertTrue(decoded.defaultBlockState().hasProperty(StabilizerBlock.HORIZONTAL_AXIS));
            assertTrue(decoded.defaultBlockState().hasProperty(StabilizerBlock.STATUS));
            assertFacing(decoded.defaultBlockState(), Direction.EAST);
            assertTrue(decoded.hasShaftTowards(
                    null, BlockPos.ZERO, decoded.defaultBlockState(), Direction.WEST));
            assertFalse(decoded.hasShaftTowards(
                    null, BlockPos.ZERO, decoded.defaultBlockState(), Direction.EAST));
        }
    }

    @Test
    void everyTierRetainsFullCubeCollisionAndOcclusion() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            BlockState state = block(tier).defaultBlockState();
            assertTrue(Block.isShapeFullBlock(state.getCollisionShape(
                    EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())));
            assertTrue(Block.isShapeFullBlock(state.getOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)));
        }
    }

    private static StabilizerBlock block(StabilizerTier tier) {
        return switch (tier) {
            case TIER_1 -> ModBlocks.TIER_1_STABILIZER.get();
            case TIER_2 -> ModBlocks.TIER_2_STABILIZER.get();
            case TIER_3 -> ModBlocks.TIER_3_STABILIZER.get();
        };
    }

    private static void assertFacing(BlockState state, Direction facing) {
        assertEquals(facing, state.getValue(StabilizerBlock.FACING));
        assertEquals(facing.getAxis(), state.getValue(StabilizerBlock.HORIZONTAL_AXIS));
    }

    private static <T extends Block> T decode(MapCodec<T> codec, JsonElement encoded)
            throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        MappedRegistry<Block> registry = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;
        Field holdersField = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
        holdersField.setAccessible(true);
        synchronized (registry) {
            @SuppressWarnings("unchecked")
            Map<Block, ?> holders = (Map<Block, ?>) holdersField.get(registry);
            Set<Block> existing = new HashSet<>(holders.keySet());
            registry.unfreeze();
            try {
                return codec.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
            } finally {
                holders.keySet().removeIf(block -> !existing.contains(block));
                registry.freeze();
            }
        }
    }
}
