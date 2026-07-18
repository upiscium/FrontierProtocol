package dev.upiscium.frontierprotocol.world;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.sector.SectorPos;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class FrontierProtocolWorldData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    public static final String DATA_NAME = FrontierProtocolMod.MOD_ID + "_world";
    public static final SavedData.Factory<FrontierProtocolWorldData> FACTORY =
            new SavedData.Factory<>(FrontierProtocolWorldData::new, FrontierProtocolWorldData::load);

    private int schemaVersion = SCHEMA_VERSION;
    private long worldSeedFingerprint;
    private int originChunkX;
    private int originChunkZ;
    private int sectorSizeChunks;
    private int placementVersion;
    private boolean initialized;
    private boolean guaranteesInitialized;
    private final Map<SectorPos, ResourceLocation> forcedTraitOverrides = new HashMap<>();
    private final Set<SectorPos> discoveredSectors = new HashSet<>();

    public static FrontierProtocolWorldData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void initialize(ServerLevel overworld) {
        if (!initialized) {
            ChunkPos spawn = new ChunkPos(overworld.getSharedSpawnPos());
            originChunkX = spawn.x;
            originChunkZ = spawn.z;
            sectorSizeChunks = FrontierProtocolServerConfig.SECTOR_SIZE_CHUNKS.getAsInt();
            placementVersion = FrontierProtocolServerConfig.PLACEMENT_VERSION.getAsInt();
            worldSeedFingerprint = fingerprint(overworld.getSeed());
            initialized = true;
            setDirty();
        } else if (sectorSizeChunks != FrontierProtocolServerConfig.SECTOR_SIZE_CHUNKS.getAsInt()) {
            FrontierProtocolMod.LOGGER.warn("World sector size {} differs from configured {}; using saved value",
                    sectorSizeChunks, FrontierProtocolServerConfig.SECTOR_SIZE_CHUNKS.getAsInt());
        }
    }

    public int originChunkX() { return originChunkX; }
    public int originChunkZ() { return originChunkZ; }
    public int sectorSizeChunks() { return sectorSizeChunks; }
    public int placementVersion() { return placementVersion; }
    public SectorPos originSector() {
        return SectorPos.fromChunk(new ChunkPos(originChunkX, originChunkZ), sectorSizeChunks);
    }
    public boolean isInitialized() { return initialized; }
    public boolean guaranteesInitialized() { return guaranteesInitialized; }
    public Map<SectorPos, ResourceLocation> forcedTraitOverrides() { return Collections.unmodifiableMap(forcedTraitOverrides); }
    public Set<SectorPos> discoveredSectors() { return Collections.unmodifiableSet(discoveredSectors); }

    public void setOverride(SectorPos pos, ResourceLocation trait) {
        forcedTraitOverrides.put(pos, trait);
        setDirty();
    }

    public void clearOverride(SectorPos pos) {
        if (forcedTraitOverrides.remove(pos) != null) setDirty();
    }

    public boolean discover(SectorPos pos) {
        boolean added = discoveredSectors.add(pos);
        if (added) setDirty();
        return added;
    }

    public void markGuaranteesInitialized() {
        guaranteesInitialized = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schemaVersion", schemaVersion);
        tag.putLong("worldSeedFingerprint", worldSeedFingerprint);
        tag.putInt("originChunkX", originChunkX);
        tag.putInt("originChunkZ", originChunkZ);
        tag.putInt("sectorSizeChunks", sectorSizeChunks);
        tag.putInt("placementVersion", placementVersion);
        tag.putBoolean("initialized", initialized);
        tag.putBoolean("guaranteesInitialized", guaranteesInitialized);

        ListTag overrides = new ListTag();
        forcedTraitOverrides.forEach((pos, trait) -> overrides.add(writeSector(pos, trait)));
        tag.put("forcedTraitOverrides", overrides);
        ListTag discovered = new ListTag();
        discoveredSectors.forEach(pos -> discovered.add(writeSector(pos, null)));
        tag.put("discoveredSectors", discovered);
        return tag;
    }

    private static FrontierProtocolWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        FrontierProtocolWorldData data = new FrontierProtocolWorldData();
        data.schemaVersion = tag.getInt("schemaVersion");
        data.worldSeedFingerprint = tag.getLong("worldSeedFingerprint");
        data.originChunkX = tag.getInt("originChunkX");
        data.originChunkZ = tag.getInt("originChunkZ");
        data.sectorSizeChunks = Math.max(1, tag.getInt("sectorSizeChunks"));
        data.placementVersion = Math.max(1, tag.getInt("placementVersion"));
        data.initialized = tag.getBoolean("initialized");
        data.guaranteesInitialized = tag.getBoolean("guaranteesInitialized");
        readOverrides(tag.getList("forcedTraitOverrides", Tag.TAG_COMPOUND), data.forcedTraitOverrides);
        readDiscovered(tag.getList("discoveredSectors", Tag.TAG_COMPOUND), data.discoveredSectors);
        return data;
    }

    private static CompoundTag writeSector(SectorPos pos, ResourceLocation trait) {
        CompoundTag value = new CompoundTag();
        value.putInt("x", pos.x());
        value.putInt("z", pos.z());
        if (trait != null) value.putString("trait", trait.toString());
        return value;
    }

    private static void readOverrides(ListTag values, Map<SectorPos, ResourceLocation> target) {
        for (Tag value : values) {
            CompoundTag compound = (CompoundTag) value;
            ResourceLocation trait = ResourceLocation.tryParse(compound.getString("trait"));
            if (trait != null) target.put(new SectorPos(compound.getInt("x"), compound.getInt("z")), trait);
        }
    }

    private static void readDiscovered(ListTag values, Set<SectorPos> target) {
        for (Tag value : values) {
            CompoundTag compound = (CompoundTag) value;
            target.add(new SectorPos(compound.getInt("x"), compound.getInt("z")));
        }
    }

    private static long fingerprint(long seed) {
        long value = seed ^ 0x9e3779b97f4a7c15L;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
