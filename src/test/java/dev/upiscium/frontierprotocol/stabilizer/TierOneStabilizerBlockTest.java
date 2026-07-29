package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
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

    @Test
    void dedicatedCodecRestoresTierOneFacingAndRearShaftContract() throws ReflectiveOperationException {
        TierOneStabilizerBlock original = block();
        assertNotSame(StabilizerBlock.CODEC, original.codec());

        JsonElement encoded = TierOneStabilizerBlock.CODEC
                .codec()
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        TierOneStabilizerBlock decoded = decode(TierOneStabilizerBlock.CODEC, encoded);

        assertInstanceOf(TierOneStabilizerBlock.class, decoded);
        assertTrue(decoded.defaultBlockState().hasProperty(TierOneStabilizerBlock.FACING));
        BlockState state = TierOneStabilizerBlock.orient(decoded.defaultBlockState(), Direction.NORTH);
        assertTrue(decoded.hasShaftTowards(null, null, state, Direction.SOUTH));
        assertFalse(decoded.hasShaftTowards(null, null, state, Direction.NORTH));
    }

    @Test
    void upperTiersRetainBaseCodecAndAxisShaftContract() throws ReflectiveOperationException {
        for (StabilizerBlock original : new StabilizerBlock[] {
            ModBlocks.TIER_2_STABILIZER.get(), ModBlocks.TIER_3_STABILIZER.get()
        }) {
            assertEquals(StabilizerBlock.class, original.getClass());
            assertFalse(original.defaultBlockState().hasProperty(TierOneStabilizerBlock.FACING));
            assertEquals(StabilizerBlock.CODEC, original.codec());

            JsonElement encoded = StabilizerBlock.CODEC
                    .codec()
                    .encodeStart(JsonOps.INSTANCE, original)
                    .getOrThrow();
            StabilizerBlock decoded = decode(StabilizerBlock.CODEC, encoded);
            BlockState state = decoded.defaultBlockState().setValue(StabilizerBlock.HORIZONTAL_AXIS, Direction.Axis.X);
            assertEquals(original.tier(), decoded.tier());
            assertFalse(state.hasProperty(TierOneStabilizerBlock.FACING));
            assertTrue(decoded.hasShaftTowards(null, null, state, Direction.EAST));
            assertTrue(decoded.hasShaftTowards(null, null, state, Direction.WEST));
            assertFalse(decoded.hasShaftTowards(null, null, state, Direction.NORTH));
        }
    }

    private static TierOneStabilizerBlock block() {
        assertTrue(ModBlocks.TIER_1_STABILIZER.isBound());
        assertFalse(ModBlocks.TIER_2_STABILIZER.get().defaultBlockState().hasProperty(TierOneStabilizerBlock.FACING));
        assertFalse(ModBlocks.TIER_3_STABILIZER.get().defaultBlockState().hasProperty(TierOneStabilizerBlock.FACING));
        return ModBlocks.TIER_1_STABILIZER.get();
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

    private static void assertAligned(BlockState state) {
        assertEquals(
                state.getValue(TierOneStabilizerBlock.FACING).getAxis(),
                state.getValue(StabilizerBlock.HORIZONTAL_AXIS));
    }
}
