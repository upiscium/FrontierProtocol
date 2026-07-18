package dev.upiscium.frontierprotocol.spawnprotection;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class SpawnProtectionSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    public static final String DATA_NAME = FrontierProtocolMod.MOD_ID + "_spawn_protection";
    public static final SavedData.Factory<SpawnProtectionSavedData> FACTORY =
            new SavedData.Factory<>(SpawnProtectionSavedData::new, SpawnProtectionSavedData::load);

    private int centerChunkX;
    private int centerChunkZ;
    private int radiusChunks;
    private boolean enabled;
    private boolean initialized;

    public static SpawnProtectionSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void initialize(ChunkPos initialSpawn, boolean configuredEnabled, int configuredRadius) {
        boolean changed = false;
        if (!initialized) {
            centerChunkX = initialSpawn.x;
            centerChunkZ = initialSpawn.z;
            initialized = true;
            changed = true;
        }
        int validatedRadius = Math.max(0, configuredRadius);
        if (enabled != configuredEnabled || radiusChunks != validatedRadius) {
            enabled = configuredEnabled;
            radiusChunks = validatedRadius;
            changed = true;
        }
        if (changed) setDirty();
    }

    public ChunkPos centerChunk() {
        return new ChunkPos(centerChunkX, centerChunkZ);
    }

    public int radiusChunks() {
        return radiusChunks;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean initialized() {
        return initialized;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schemaVersion", SCHEMA_VERSION);
        tag.putInt("centerChunkX", centerChunkX);
        tag.putInt("centerChunkZ", centerChunkZ);
        tag.putInt("radiusChunks", radiusChunks);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    static SpawnProtectionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SpawnProtectionSavedData data = new SpawnProtectionSavedData();
        data.centerChunkX = tag.getInt("centerChunkX");
        data.centerChunkZ = tag.getInt("centerChunkZ");
        data.radiusChunks = Math.max(0, tag.getInt("radiusChunks"));
        data.enabled = tag.getBoolean("enabled");
        data.initialized = tag.getBoolean("initialized");
        return data;
    }
}
