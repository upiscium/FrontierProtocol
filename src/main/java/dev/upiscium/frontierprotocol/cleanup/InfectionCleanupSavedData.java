package dev.upiscium.frontierprotocol.cleanup;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class InfectionCleanupSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    public static final String DATA_NAME = FrontierProtocolMod.MOD_ID + "_cleanup_progress";
    public static final SavedData.Factory<InfectionCleanupSavedData> FACTORY =
            new SavedData.Factory<>(InfectionCleanupSavedData::new, InfectionCleanupSavedData::load);

    private static final String CHUNKS_KEY = "chunks";
    private final Map<Long, CleanupProgress> progressByChunk = new HashMap<>();

    public static InfectionCleanupSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public CleanupProgress progress(long chunkKey, int minSection, int sectionCount) {
        CleanupProgress progress = progressByChunk.get(chunkKey);
        if (progress == null) {
            progress = CleanupProgress.start(minSection, sectionCount, false);
            progressByChunk.put(chunkKey, progress);
            setDirty();
        } else if (progress.minSection() != minSection || progress.sectionCount() != sectionCount) {
            FrontierProtocolMod.LOGGER.warn(
                    "Resetting cleanup cursor for chunk {},{} because dimension height changed",
                    ChunkPos.getX(chunkKey),
                    ChunkPos.getZ(chunkKey));
            progress = CleanupProgress.start(minSection, sectionCount, false);
            progressByChunk.put(chunkKey, progress);
            setDirty();
        }
        return progress;
    }

    public void update(long chunkKey, CleanupProgress progress) {
        if (!progress.equals(progressByChunk.put(chunkKey, progress))) setDirty();
    }

    public CleanupProgress reset(long chunkKey, int minSection, int sectionCount) {
        CleanupProgress progress = CleanupProgress.start(minSection, sectionCount, false);
        update(chunkKey, progress);
        return progress;
    }

    public CleanupProgress resume(long chunkKey, int minSection, int sectionCount) {
        CleanupProgress progress = progress(chunkKey, minSection, sectionCount).activate(CleanupActivationMode.RESUME);
        update(chunkKey, progress);
        return progress;
    }

    public CleanupProgress activate(
            long chunkKey, int minSection, int sectionCount, CleanupActivationMode activationMode) {
        CleanupProgress progress = progress(chunkKey, minSection, sectionCount).activate(activationMode);
        update(chunkKey, progress);
        return progress;
    }

    public void markRestartRequired(long chunkKey, int minSection, int sectionCount) {
        CleanupProgress progress = progress(chunkKey, minSection, sectionCount).withRestartRequired(true);
        update(chunkKey, progress);
    }

    public Map<Long, CleanupProgress> snapshot() {
        return Map.copyOf(progressByChunk);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schemaVersion", SCHEMA_VERSION);
        ListTag chunks = new ListTag();
        for (Map.Entry<Long, CleanupProgress> entry : progressByChunk.entrySet()) {
            CleanupProgress progress = entry.getValue();
            CompoundTag chunk = new CompoundTag();
            chunk.putInt("chunkX", ChunkPos.getX(entry.getKey()));
            chunk.putInt("chunkZ", ChunkPos.getZ(entry.getKey()));
            chunk.putInt("sectionIndex", progress.cursor().sectionIndex());
            chunk.putInt("localBlockIndex", progress.cursor().localBlockIndex());
            chunk.putBoolean("completed", progress.cursor().completed());
            chunk.putBoolean("restartRequired", progress.restartRequired());
            chunk.putInt("minSection", progress.minSection());
            chunk.putInt("sectionCount", progress.sectionCount());
            chunks.add(chunk);
        }
        tag.put(CHUNKS_KEY, chunks);
        return tag;
    }

    static InfectionCleanupSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("schemaVersion", Tag.TAG_INT)) {
            throw new IllegalStateException("Invalid cleanup schemaVersion");
        }
        int schemaVersion = tag.getInt("schemaVersion");
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported cleanup schema: " + schemaVersion);
        }
        if (!tag.contains(CHUNKS_KEY, Tag.TAG_LIST)) {
            throw new IllegalStateException("Invalid cleanup chunks list");
        }

        InfectionCleanupSavedData data = new InfectionCleanupSavedData();
        ListTag chunks = tag.getList(CHUNKS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < chunks.size(); i++) {
            CompoundTag chunk = chunks.getCompound(i);
            if (!chunk.contains("chunkX", Tag.TAG_INT) || !chunk.contains("chunkZ", Tag.TAG_INT)) {
                FrontierProtocolMod.LOGGER.warn("Skipping cleanup progress with invalid chunk coordinates");
                data.setDirty();
                continue;
            }
            int chunkX = chunk.getInt("chunkX");
            int chunkZ = chunk.getInt("chunkZ");
            long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
            CleanupProgress progress = readProgress(chunk, chunkX, chunkZ);
            data.progressByChunk.put(chunkKey, progress);
            if (!isValidProgress(chunk)) data.setDirty();
        }
        return data;
    }

    private static CleanupProgress readProgress(CompoundTag tag, int chunkX, int chunkZ) {
        if (!hasRequiredFields(tag)) return invalidProgress(chunkX, chunkZ, 0, 1);

        int minSection = tag.getInt("minSection");
        int sectionCount = tag.getInt("sectionCount");
        int sectionIndex = tag.getInt("sectionIndex");
        int localBlockIndex = tag.getInt("localBlockIndex");
        boolean completed = tag.getBoolean("completed");
        if (sectionCount <= 0
                || sectionIndex < 0
                || sectionIndex >= sectionCount
                || localBlockIndex < 0
                || localBlockIndex >= CleanupCursor.BLOCKS_PER_SECTION
                || (completed
                        && (sectionIndex != sectionCount - 1
                                || localBlockIndex != CleanupCursor.BLOCKS_PER_SECTION - 1))) {
            return invalidProgress(chunkX, chunkZ, minSection, Math.max(sectionCount, 1));
        }

        CleanupCursor cursor = new CleanupCursor(sectionIndex, localBlockIndex, completed);
        return new CleanupProgress(cursor, tag.getBoolean("restartRequired"), minSection, sectionCount);
    }

    private static boolean hasRequiredFields(CompoundTag tag) {
        return tag.contains("chunkX", Tag.TAG_INT)
                && tag.contains("chunkZ", Tag.TAG_INT)
                && tag.contains("sectionIndex", Tag.TAG_INT)
                && tag.contains("localBlockIndex", Tag.TAG_INT)
                && tag.contains("completed", Tag.TAG_BYTE)
                && tag.contains("restartRequired", Tag.TAG_BYTE)
                && tag.contains("minSection", Tag.TAG_INT)
                && tag.contains("sectionCount", Tag.TAG_INT);
    }

    private static boolean isValidProgress(CompoundTag tag) {
        if (!hasRequiredFields(tag)) return false;
        int sectionCount = tag.getInt("sectionCount");
        int sectionIndex = tag.getInt("sectionIndex");
        int localBlockIndex = tag.getInt("localBlockIndex");
        boolean completed = tag.getBoolean("completed");
        return sectionCount > 0
                && sectionIndex >= 0
                && sectionIndex < sectionCount
                && localBlockIndex >= 0
                && localBlockIndex < CleanupCursor.BLOCKS_PER_SECTION
                && (!completed
                        || (sectionIndex == sectionCount - 1
                                && localBlockIndex == CleanupCursor.BLOCKS_PER_SECTION - 1));
    }

    private static CleanupProgress invalidProgress(int chunkX, int chunkZ, int minSection, int sectionCount) {
        FrontierProtocolMod.LOGGER.warn("Resetting invalid cleanup cursor for chunk {},{}", chunkX, chunkZ);
        return CleanupProgress.start(minSection, sectionCount, true);
    }
}
