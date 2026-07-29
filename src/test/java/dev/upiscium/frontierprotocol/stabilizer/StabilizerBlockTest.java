package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
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
    void defaultsContainOnlyFacingAndStatusDirectionProperties() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            BlockState state = block(tier).defaultBlockState();
            assertEquals(Direction.EAST, state.getValue(StabilizerBlock.FACING));
            assertEquals(StabilizerStatus.OFFLINE, state.getValue(StabilizerBlock.STATUS));
            assertTrue(state.hasProperty(StabilizerBlock.FACING));
            assertTrue(state.hasProperty(StabilizerBlock.STATUS));
            assertFalse(state.hasProperty(HorizontalAxisKineticBlock.HORIZONTAL_AXIS));
            assertEquals(Set.of(StabilizerBlock.FACING, StabilizerBlock.STATUS), Set.copyOf(state.getProperties()));
        }
    }

    @Test
    void rotationAxisAndBothSideShaftsAreDerivedFromFacing() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerBlock block = block(tier);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = block.defaultBlockState().setValue(StabilizerBlock.FACING, facing);
                assertShaftContract(block, state, facing.getClockWise().getAxis());
            }
        }
    }

    @Test
    void rotationMirrorAndStatusChangesPreserveDerivedShaftContract() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerBlock block = block(tier);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = block.defaultBlockState().setValue(StabilizerBlock.FACING, facing);
                for (Rotation rotation : Rotation.values()) {
                    BlockState rotated = block.rotate(state, rotation);
                    assertEquals(rotation.rotate(facing), rotated.getValue(StabilizerBlock.FACING));
                    assertShaftContract(block, rotated, rotation.rotate(facing).getClockWise().getAxis());
                }
                for (Mirror mirror : Mirror.values()) {
                    BlockState mirrored = block.mirror(state, mirror);
                    assertEquals(mirror.mirror(facing), mirrored.getValue(StabilizerBlock.FACING));
                    assertShaftContract(
                            block,
                            mirrored,
                            mirrored.getValue(StabilizerBlock.FACING).getClockWise().getAxis());
                }
                for (StabilizerStatus status : StabilizerStatus.values()) {
                    BlockState changed = state.setValue(StabilizerBlock.STATUS, status);
                    assertEquals(facing, changed.getValue(StabilizerBlock.FACING));
                    assertShaftContract(block, changed, facing.getClockWise().getAxis());
                }
            }
        }
    }

    @Test
    void topFaceWrenchRotationMovesFacingAxisAndShaftFaces() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerBlock block = block(tier);
            BlockState initial = block.defaultBlockState().setValue(StabilizerBlock.STATUS, StabilizerStatus.ACTIVE);
            assertShaftContract(block, initial, Direction.Axis.Z);

            BlockState rotated = block.getRotatedBlockState(initial, Direction.UP);

            assertNotEquals(Direction.EAST, rotated.getValue(StabilizerBlock.FACING));
            assertTrue(rotated.getValue(StabilizerBlock.FACING).getAxis() == Direction.Axis.Z);
            assertEquals(StabilizerStatus.ACTIVE, rotated.getValue(StabilizerBlock.STATUS));
            assertShaftContract(block, rotated, Direction.Axis.X);
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
            assertTrue(decoded.defaultBlockState().hasProperty(StabilizerBlock.STATUS));
            assertFalse(decoded.defaultBlockState().hasProperty(HorizontalAxisKineticBlock.HORIZONTAL_AXIS));
            assertEquals(Direction.EAST, decoded.defaultBlockState().getValue(StabilizerBlock.FACING));
            assertEquals(StabilizerStatus.OFFLINE, decoded.defaultBlockState().getValue(StabilizerBlock.STATUS));
            assertShaftContract(decoded, decoded.defaultBlockState(), Direction.Axis.Z);
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

    private static void assertShaftContract(
            StabilizerBlock block, BlockState state, Direction.Axis expectedAxis) {
        assertEquals(expectedAxis, block.getRotationAxis(state));
        for (Direction face : Direction.values()) {
            assertEquals(
                    face.getAxis() == expectedAxis,
                    block.hasShaftTowards(null, BlockPos.ZERO, state, face),
                    "unexpected shaft contract for " + state + " at " + face);
        }
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
