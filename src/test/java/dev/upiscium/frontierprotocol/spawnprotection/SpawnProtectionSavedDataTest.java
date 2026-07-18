package dev.upiscium.frontierprotocol.spawnprotection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class SpawnProtectionSavedDataTest {
    @Test
    void nbtRoundTripPreservesCenterAndSettings() {
        SpawnProtectionSavedData original = new SpawnProtectionSavedData();
        original.initialize(new ChunkPos(-12, 9), true, 2);
        CompoundTag tag = original.save(new CompoundTag(), null);

        SpawnProtectionSavedData restored = SpawnProtectionSavedData.load(tag, null);

        assertTrue(restored.initialized());
        assertTrue(restored.enabled());
        assertEquals(new ChunkPos(-12, 9), restored.centerChunk());
        assertEquals(2, restored.radiusChunks());
    }

    @Test
    void reinitializationUpdatesSettingsButNotInitialCenter() {
        SpawnProtectionSavedData data = new SpawnProtectionSavedData();
        data.initialize(new ChunkPos(-4, -5), true, 2);

        data.initialize(new ChunkPos(30, 40), false, 4);

        assertEquals(new ChunkPos(-4, -5), data.centerChunk());
        assertFalse(data.enabled());
        assertEquals(4, data.radiusChunks());
    }

    @Test
    void fiveByFiveCoverageWorksAtNegativeCoordinates() {
        var chunks = SpawnProtectionManager.coveredChunks(new ChunkPos(-2, -3), 2);
        assertEquals(25, chunks.size());
        assertTrue(chunks.contains(new ChunkPos(-4, -5)));
        assertTrue(chunks.contains(new ChunkPos(0, -1)));
        assertFalse(chunks.contains(new ChunkPos(1, -3)));
    }
}
