package dev.upiscium.frontierprotocol.spawnprotection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class SpawnProtectionSavedDataTest {
    @Test
    void repeatedInitializationKeepsOriginalCenter() {
        SpawnProtectionSavedData data = new SpawnProtectionSavedData();
        data.initialize(new ChunkPos(-8, 3));
        data.initialize(new ChunkPos(42, 42));
        assertEquals(new ChunkPos(-8, 3), data.centerChunk());
    }

    @Test
    void schemaTwoRoundTripPreservesOnlyInitialCenter() {
        SpawnProtectionSavedData original = new SpawnProtectionSavedData();
        original.initialize(new ChunkPos(-12, 9));
        CompoundTag tag = original.save(new CompoundTag(), null);

        SpawnProtectionSavedData restored = SpawnProtectionSavedData.load(tag, null);

        assertEquals(SpawnProtectionSavedData.SCHEMA_VERSION, tag.getInt("schemaVersion"));
        assertFalse(tag.contains("enabled"));
        assertFalse(tag.contains("radiusChunks"));
        assertTrue(restored.initialized());
        assertEquals(new ChunkPos(-12, 9), restored.centerChunk());
    }

    @Test
    void schemaOneMigratesCenterAndDiscardsLegacySettings() {
        CompoundTag legacy = centerTag(-8, 6);
        legacy.putInt("schemaVersion", 1);
        legacy.putBoolean("enabled", false);
        legacy.putInt("radiusChunks", -4);

        SpawnProtectionSavedData restored = SpawnProtectionSavedData.load(legacy, null);
        CompoundTag migrated = restored.save(new CompoundTag(), null);

        assertEquals(new ChunkPos(-8, 6), restored.centerChunk());
        assertEquals(SpawnProtectionSavedData.SCHEMA_VERSION, migrated.getInt("schemaVersion"));
        assertFalse(migrated.contains("enabled"));
        assertFalse(migrated.contains("radiusChunks"));
    }

    @Test
    void missingSchemaVersionIsExplicitlyTreatedAsLegacySchemaOne() {
        CompoundTag versionless = centerTag(3, -7);
        versionless.putBoolean("enabled", true);
        versionless.putInt("radiusChunks", 9);

        SpawnProtectionSavedData restored = SpawnProtectionSavedData.load(versionless, null);

        assertTrue(restored.initialized());
        assertEquals(new ChunkPos(3, -7), restored.centerChunk());
    }

    @Test
    void unknownSchemaIsRejectedInsteadOfReadAsSchemaOne() {
        CompoundTag future = centerTag(1, 2);
        future.putInt("schemaVersion", SpawnProtectionSavedData.SCHEMA_VERSION + 1);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> SpawnProtectionSavedData.load(future, null));

        assertTrue(error.getMessage().contains("Unsupported spawn protection schema"));
    }

    @Test
    void reinitializationDoesNotChangeInitialCenter() {
        SpawnProtectionSavedData data = new SpawnProtectionSavedData();
        data.initialize(new ChunkPos(-4, -5));

        data.initialize(new ChunkPos(30, 40));

        assertEquals(new ChunkPos(-4, -5), data.centerChunk());
    }

    @Test
    void fiveByFiveCoverageWorksAtNegativeCoordinates() {
        var chunks = SpawnProtectionManager.coveredChunks(new ChunkPos(-2, -3), 2);
        assertEquals(25, chunks.size());
        assertTrue(chunks.contains(new ChunkPos(-4, -5)));
        assertTrue(chunks.contains(new ChunkPos(0, -1)));
        assertFalse(chunks.contains(new ChunkPos(1, -3)));
    }

    @Test
    void radiusZeroCoversOnlyTheCenterChunk() {
        ChunkPos center = new ChunkPos(5, -6);

        var chunks = SpawnProtectionManager.coveredChunks(center, 0);

        assertEquals(1, chunks.size());
        assertTrue(chunks.contains(center));
    }

    private static CompoundTag centerTag(int x, int z) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("centerChunkX", x);
        tag.putInt("centerChunkZ", z);
        tag.putBoolean("initialized", true);
        return tag;
    }
}
