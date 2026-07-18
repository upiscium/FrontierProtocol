package dev.upiscium.frontierprotocol.spawnprotection;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class SpawnProtectionSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 2;
    private static final int LEGACY_SCHEMA_VERSION = 1;
    public static final String DATA_NAME = FrontierProtocolMod.MOD_ID + "_spawn_protection";
    public static final SavedData.Factory<SpawnProtectionSavedData> FACTORY =
            new SavedData.Factory<>(SpawnProtectionSavedData::new, SpawnProtectionSavedData::load);

    private int centerChunkX;
    private int centerChunkZ;
    private boolean initialized;

    public static SpawnProtectionSavedData get(ServerLevel level) {
        requireOverworld(level);
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void initialize(ChunkPos initialSpawn) {
        if (!initialized) {
            centerChunkX = initialSpawn.x;
            centerChunkZ = initialSpawn.z;
            initialized = true;
            setDirty();
        }
    }

    public ChunkPos centerChunk() {
        return new ChunkPos(centerChunkX, centerChunkZ);
    }

    public boolean initialized() {
        return initialized;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schemaVersion", SCHEMA_VERSION);
        tag.putInt("centerChunkX", centerChunkX);
        tag.putInt("centerChunkZ", centerChunkZ);
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    static SpawnProtectionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        int schemaVersion;
        if (!tag.contains("schemaVersion")) {
            schemaVersion = LEGACY_SCHEMA_VERSION;
        } else if (tag.contains("schemaVersion", Tag.TAG_INT)) {
            schemaVersion = tag.getInt("schemaVersion");
        } else {
            throw new IllegalStateException("Invalid spawn protection schemaVersion type");
        }
        return switch (schemaVersion) {
            case LEGACY_SCHEMA_VERSION -> loadLegacySchema(tag);
            case SCHEMA_VERSION -> loadCurrentSchema(tag);
            // Refuse future schemas rather than silently interpreting and potentially overwriting them.
            default -> throw new IllegalStateException("Unsupported spawn protection schema: " + schemaVersion);
        };
    }

    private static SpawnProtectionSavedData loadLegacySchema(CompoundTag tag) {
        SpawnProtectionSavedData data = loadCenter(tag);
        data.setDirty();
        return data;
    }

    private static SpawnProtectionSavedData loadCurrentSchema(CompoundTag tag) {
        return loadCenter(tag);
    }

    private static SpawnProtectionSavedData loadCenter(CompoundTag tag) {
        SpawnProtectionSavedData data = new SpawnProtectionSavedData();
        data.centerChunkX = tag.getInt("centerChunkX");
        data.centerChunkZ = tag.getInt("centerChunkZ");
        data.initialized = tag.getBoolean("initialized");
        return data;
    }

    private static void requireOverworld(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            throw new IllegalArgumentException("Spawn protection SavedData is Overworld-only");
        }
    }
}
