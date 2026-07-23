package dev.upiscium.frontierprotocol.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class InfectionCleanupSavedDataTest {
    @Test
    void schemaOneRoundTripPreservesNegativeChunkAndIncompleteCursor() {
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        long chunkKey = ChunkPos.asLong(-17, -31);
        data.update(
                chunkKey,
                new CleanupProgress(new CleanupCursor(3, 2048, false), false, -4, 24));

        CompoundTag saved = data.save(new CompoundTag(), null);
        InfectionCleanupSavedData restored = InfectionCleanupSavedData.load(saved, null);

        assertEquals(InfectionCleanupSavedData.SCHEMA_VERSION, saved.getInt("schemaVersion"));
        assertEquals(data.snapshot(), restored.snapshot());
        assertEquals(3, restored.snapshot().get(chunkKey).cursor().sectionIndex());
        assertEquals(2048, restored.snapshot().get(chunkKey).cursor().localBlockIndex());
    }

    @Test
    void completedAndRestartRequiredRoundTrip() {
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        long completedKey = ChunkPos.asLong(1, 2);
        long restartKey = ChunkPos.asLong(-2, 9);
        data.update(
                completedKey,
                new CleanupProgress(new CleanupCursor(15, 4095, true), false, -4, 16));
        data.update(restartKey, new CleanupProgress(new CleanupCursor(2, 33, false), true, 0, 8));

        InfectionCleanupSavedData restored =
                InfectionCleanupSavedData.load(data.save(new CompoundTag(), null), null);
        assertTrue(restored.snapshot().get(completedKey).cursor().completed());
        assertTrue(restored.snapshot().get(restartKey).restartRequired());
    }

    @Test
    void runtimeSourcesAreNeverSerialized() {
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        data.update(ChunkPos.asLong(0, 0), CleanupProgress.start(-4, 24, false));
        CompoundTag saved = data.save(new CompoundTag(), null);

        assertFalse(saved.contains("sources"));
        assertFalse(saved.toString().contains("sourceId"));
        assertFalse(saved.toString().contains("activeSources"));
    }

    @Test
    void unknownOrMissingSchemaIsRejected() {
        CompoundTag missing = new CompoundTag();
        missing.put("chunks", new ListTag());
        assertThrows(IllegalStateException.class, () -> InfectionCleanupSavedData.load(missing, null));

        CompoundTag future = new CompoundTag();
        future.putInt("schemaVersion", InfectionCleanupSavedData.SCHEMA_VERSION + 1);
        future.put("chunks", new ListTag());
        assertThrows(IllegalStateException.class, () -> InfectionCleanupSavedData.load(future, null));
    }

    @Test
    void invalidCursorResetsToStartAndRequiresNewPass() {
        CompoundTag saved = savedWithCursor(-5, 6, 0, 4096, -4, 24);
        InfectionCleanupSavedData restored = InfectionCleanupSavedData.load(saved, null);
        CleanupProgress progress = restored.snapshot().get(ChunkPos.asLong(-5, 6));

        assertEquals(CleanupCursor.start(), progress.cursor());
        assertTrue(progress.restartRequired());
        assertTrue(restored.isDirty());
    }

    @Test
    void dimensionHeightChangeResetsToCurrentMinimum() {
        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        long chunkKey = ChunkPos.asLong(4, -8);
        data.update(chunkKey, new CleanupProgress(new CleanupCursor(3, 100, false), false, -4, 24));

        CleanupProgress reset = data.progress(chunkKey, 0, 16);
        assertEquals(CleanupCursor.start(), reset.cursor());
        assertEquals(0, reset.minSection());
        assertEquals(16, reset.sectionCount());
    }

    @Test
    void activationModesResetOrResumeAsSpecified() {
        CleanupProgress interrupted =
                new CleanupProgress(new CleanupCursor(4, 900, false), false, -4, 24);
        assertEquals(
                new CleanupCursor(4, 900, false),
                interrupted.activate(CleanupActivationMode.RESUME).cursor());
        assertFalse(interrupted.activate(CleanupActivationMode.RESUME).restartRequired());
        assertEquals(CleanupCursor.start(), interrupted.activate(CleanupActivationMode.NEW_PASS).cursor());

        CleanupProgress deactivated = interrupted.withRestartRequired(true);
        assertEquals(CleanupCursor.start(), deactivated.activate(CleanupActivationMode.RESUME).cursor());
    }

    @Test
    void malformedCompletedCursorIsResetInsteadOfSkippingChunk() {
        CompoundTag saved = savedWithCursor(8, -3, 2, 100, -4, 24);
        saved.getList("chunks", CompoundTag.TAG_COMPOUND).getCompound(0).putBoolean("completed", true);

        InfectionCleanupSavedData restored = InfectionCleanupSavedData.load(saved, null);
        CleanupProgress progress = restored.snapshot().get(ChunkPos.asLong(8, -3));
        assertEquals(CleanupCursor.start(), progress.cursor());
        assertTrue(progress.restartRequired());
    }

    private static CompoundTag savedWithCursor(
            int chunkX, int chunkZ, int sectionIndex, int localIndex, int minSection, int sectionCount) {
        CompoundTag chunk = new CompoundTag();
        chunk.putInt("chunkX", chunkX);
        chunk.putInt("chunkZ", chunkZ);
        chunk.putInt("sectionIndex", sectionIndex);
        chunk.putInt("localBlockIndex", localIndex);
        chunk.putBoolean("completed", false);
        chunk.putBoolean("restartRequired", false);
        chunk.putInt("minSection", minSection);
        chunk.putInt("sectionCount", sectionCount);
        ListTag chunks = new ListTag();
        chunks.add(chunk);
        CompoundTag saved = new CompoundTag();
        saved.putInt("schemaVersion", InfectionCleanupSavedData.SCHEMA_VERSION);
        saved.put("chunks", chunks);
        return saved;
    }
}
