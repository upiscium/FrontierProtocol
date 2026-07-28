package dev.upiscium.frontierprotocol.stabilizer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class StabilizerDisplayNbtTest {
    @Test
    void roundTripPreservesDisplaySchemaAndFields() {
        StabilizerDisplaySnapshot expected = snapshot();
        CompoundTag root = new CompoundTag();

        StabilizerDisplayNbt.write(root, expected);

        CompoundTag display = root.getCompound(StabilizerDisplayNbt.DISPLAY_KEY);
        assertEquals(StabilizerDisplayNbt.SCHEMA_VERSION, display.getInt("schemaVersion"));
        assertEquals("tier_2", display.getString("tier"));
        assertEquals("grace_period", display.getString("status"));
        assertEquals(expected, StabilizerDisplayNbt.read(root).orElseThrow());
    }

    @Test
    void rejectsMissingUnknownNegativeAndMalformedValues() {
        assertTrue(StabilizerDisplayNbt.read(new CompoundTag()).isEmpty());

        CompoundTag root = written();
        root.getCompound(StabilizerDisplayNbt.DISPLAY_KEY).putString("tier", "future_tier");
        assertTrue(StabilizerDisplayNbt.read(root).isEmpty());

        root = written();
        root.getCompound(StabilizerDisplayNbt.DISPLAY_KEY).putString("status", "future_status");
        assertTrue(StabilizerDisplayNbt.read(root).isEmpty());

        root = written();
        root.getCompound(StabilizerDisplayNbt.DISPLAY_KEY).putInt("cellCount", -1);
        assertTrue(StabilizerDisplayNbt.read(root).isEmpty());

        root = written();
        root.getCompound(StabilizerDisplayNbt.DISPLAY_KEY).remove("chunkRadius");
        assertTrue(StabilizerDisplayNbt.read(root).isEmpty());

        root = new CompoundTag();
        root.putString(StabilizerDisplayNbt.DISPLAY_KEY, "invalid");
        assertTrue(StabilizerDisplayNbt.read(root).isEmpty());
    }

    @Test
    void invalidPacketCanPreserveLastValidSnapshot() {
        StabilizerDisplaySnapshot previous = snapshot();
        StabilizerDisplaySnapshot retained = StabilizerDisplayNbt.readOrRetain(
                new CompoundTag(), StabilizerTier.TIER_2, previous);
        assertEquals(previous, retained);

        CompoundTag wrongTier = written();
        wrongTier.getCompound(StabilizerDisplayNbt.DISPLAY_KEY).putString("tier", "tier_3");
        assertEquals(
                previous,
                StabilizerDisplayNbt.readOrRetain(wrongTier, StabilizerTier.TIER_2, previous));
    }

    @Test
    void roundTripAcceptsInventoryAboveCurrentConfiguredCapacity() {
        StabilizerDisplaySnapshot expected = new StabilizerDisplaySnapshot(
                StabilizerTier.TIER_2,
                StabilizerStatus.OFFLINE,
                64,
                64.0,
                32,
                8,
                0,
                3000,
                0,
                1);
        CompoundTag root = new CompoundTag();
        StabilizerDisplayNbt.write(root, expected);

        StabilizerDisplaySnapshot restored = StabilizerDisplayNbt.read(root).orElseThrow();
        assertEquals(32, restored.cellCount());
        assertEquals(8, restored.cellCapacity());
        assertEquals(expected, StabilizerDisplayNbt.readOrRetain(root, StabilizerTier.TIER_2, snapshot()));
    }

    @Test
    void validatesPacketRadiusAndRetainsPreviousSnapshot() {
        CompoundTag maximum = written();
        maximum.getCompound(StabilizerDisplayNbt.DISPLAY_KEY).putInt("chunkRadius", 16);
        assertEquals(16, StabilizerDisplayNbt.read(maximum).orElseThrow().chunkRadius());

        StabilizerDisplaySnapshot previous = snapshot();
        for (int invalidRadius : new int[] {17, Integer.MAX_VALUE}) {
            CompoundTag invalid = written();
            invalid.getCompound(StabilizerDisplayNbt.DISPLAY_KEY).putInt("chunkRadius", invalidRadius);
            assertTrue(StabilizerDisplayNbt.read(invalid).isEmpty());
            assertEquals(
                    previous,
                    StabilizerDisplayNbt.readOrRetain(invalid, StabilizerTier.TIER_2, previous));
        }
    }

    private static CompoundTag written() {
        CompoundTag root = new CompoundTag();
        StabilizerDisplayNbt.write(root, snapshot());
        return root;
    }

    private static StabilizerDisplaySnapshot snapshot() {
        return new StabilizerDisplaySnapshot(
                StabilizerTier.TIER_2,
                StabilizerStatus.GRACE_PERIOD,
                64,
                64.0,
                12,
                32,
                1200,
                3000,
                900,
                1);
    }
}
