package dev.upiscium.frontierprotocol.migration;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

final class MigrationWorldInspector {
    private static final String SPAWN_DATA = "frontier_protocol_spawn_protection.dat";
    private static final String CLEANUP_DATA = "frontier_protocol_cleanup_progress.dat";

    Snapshot inspect(Path world, MigrationFixtureManifest manifest) throws IOException {
        require(Files.isRegularFile(world.resolve("level.dat")), "level.dat is missing");
        CompoundTag level = readCompressed(world.resolve("level.dat"));
        require(level.contains("Data", Tag.TAG_COMPOUND), "level.dat has no Data compound");
        CompoundTag levelData = level.getCompound("Data");
        require(levelData.contains("WorldGenSettings", Tag.TAG_COMPOUND),
                "level.dat has no vanilla WorldGenSettings compound");
        CompoundTag worldGenSettings = levelData.getCompound("WorldGenSettings");
        require(worldGenSettings.contains("seed", Tag.TAG_LONG), "level.dat has no vanilla world seed");
        long persistedLevelSeed = worldGenSettings.getLong("seed");

        verifyAllRegionChunks(world.resolve("region"), false);
        verifyAllRegionChunks(world.resolve("entities"), true);
        verifyAllRegionChunks(world.resolve("poi"), false);
        List<StabilizerState> stabilizers = new ArrayList<>();
        Map<Long, CompoundTag> chunks = new HashMap<>();
        for (MigrationFixtureManifest.StabilizerExpectation expected : manifest.stabilizers()) {
            CompoundTag chunk = chunks.computeIfAbsent(
                    chunkKey(expected.position().chunkX(), expected.position().chunkZ()),
                    ignored -> uncheckedChunk(world, expected.position().chunkX(), expected.position().chunkZ()));
            BlockStateAtPosition block = blockState(chunk, expected.position());
            CompoundTag blockEntity = blockEntity(chunk, expected.position());
            Map<String, Integer> inventory = itemCounts(blockEntity.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND));
            stabilizers.add(new StabilizerState(
                    expected.position(),
                    block.name(),
                    block.properties().get("facing"),
                    block.properties().get("status"),
                    blockEntity.getString("id"),
                    blockEntity.getInt("schemaVersion"),
                    blockEntity.getString("tier"),
                    blockEntity.getString("status"),
                    blockEntity.getInt("cellRemainingTicks"),
                    blockEntity.getInt("graceRemainingTicks"),
                    blockEntity.getInt("registeredChunkRadius"),
                    inventory));
        }

        MigrationFixtureManifest.Position containerPosition = manifest.container().position();
        CompoundTag containerChunk = chunks.computeIfAbsent(
                chunkKey(containerPosition.chunkX(), containerPosition.chunkZ()),
                ignored -> uncheckedChunk(world, containerPosition.chunkX(), containerPosition.chunkZ()));
        BlockStateAtPosition containerBlock = blockState(containerChunk, containerPosition);
        CompoundTag containerEntity = blockEntity(containerChunk, containerPosition);
        ContainerState container = new ContainerState(
                containerPosition,
                containerBlock.name(),
                containerEntity.getString("id"),
                itemCounts(containerEntity.getList("Items", Tag.TAG_COMPOUND)));

        Path dataDirectory = world.resolve("data");
        long spawnFiles;
        try (var files = Files.list(dataDirectory)) {
            spawnFiles = files.filter(path -> path.getFileName().toString().startsWith("frontier_protocol_spawn_protection"))
                    .count();
        }
        CompoundTag spawn = readCompressed(dataDirectory.resolve(SPAWN_DATA)).getCompound("data");
        SpawnState spawnState = new SpawnState(
                spawn.getInt("schemaVersion"),
                spawn.getBoolean("initialized"),
                spawn.getInt("centerChunkX"),
                spawn.getInt("centerChunkZ"),
                spawnFiles);

        CompoundTag cleanup = readCompressed(dataDirectory.resolve(CLEANUP_DATA)).getCompound("data");
        ListTag cleanupChunks = cleanup.getList("chunks", Tag.TAG_COMPOUND);
        CompoundTag expectedCleanup = null;
        for (int index = 0; index < cleanupChunks.size(); index++) {
            CompoundTag entry = cleanupChunks.getCompound(index);
            if (entry.getInt("chunkX") == manifest.cleanup().chunkX()
                    && entry.getInt("chunkZ") == manifest.cleanup().chunkZ()) {
                expectedCleanup = entry;
                break;
            }
        }
        require(expectedCleanup != null, "Expected cleanup progress disappeared");
        CleanupState cleanupState = new CleanupState(
                cleanup.getInt("schemaVersion"),
                expectedCleanup.getInt("chunkX"),
                expectedCleanup.getInt("chunkZ"),
                expectedCleanup.getInt("sectionIndex"),
                expectedCleanup.getInt("localBlockIndex"),
                expectedCleanup.getBoolean("completed"),
                expectedCleanup.getBoolean("restartRequired"),
                expectedCleanup.getInt("minSection"),
                expectedCleanup.getInt("sectionCount"));

        return new Snapshot(persistedLevelSeed, List.copyOf(stabilizers), container, spawnState, cleanupState);
    }

    private static CompoundTag uncheckedChunk(Path world, int chunkX, int chunkZ) {
        try {
            return readChunk(world, chunkX, chunkZ);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read documented chunk " + chunkX + "," + chunkZ, exception);
        }
    }

    private static void verifyAllRegionChunks(Path regionDirectory, boolean inspectEntities) throws IOException {
        require(Files.isDirectory(regionDirectory), "Required region directory is missing: " + regionDirectory.getFileName());
        try (var regions = Files.list(regionDirectory)) {
            for (Path region : regions.filter(path -> path.getFileName().toString().endsWith(".mca")).sorted().toList()) {
                verifyRegionFile(region, inspectEntities);
            }
        }
    }

    private static void verifyRegionFile(Path region, boolean inspectEntities) throws IOException {
        byte[] bytes = Files.readAllBytes(region);
        require(bytes.length >= 8192 && bytes.length % 4096 == 0, "Invalid region file size: " + region.getFileName());
        ByteBuffer header = ByteBuffer.wrap(bytes, 0, 4096).order(ByteOrder.BIG_ENDIAN);
        for (int index = 0; index < 1024; index++) {
            int location = header.getInt(index * 4);
            if (location == 0) continue;
            int sector = location >>> 8;
            int sectors = location & 0xff;
            require(sector >= 2 && sectors > 0 && (long) (sector + sectors) * 4096 <= bytes.length,
                    "Invalid chunk location in " + region.getFileName());
            CompoundTag chunk = readChunkPayload(bytes, sector * 4096);
            if (inspectEntities) {
                ListTag entities = chunk.getList("Entities", Tag.TAG_COMPOUND);
                for (int entityIndex = 0; entityIndex < entities.size(); entityIndex++) {
                    CompoundTag entity = entities.getCompound(entityIndex);
                    if (entity.getString("id").equals("minecraft:item")) {
                        String itemId = entity.getCompound("Item").getString("id");
                        require(!itemId.startsWith("frontier_protocol:"),
                                "Unexpected dropped Frontier Protocol item entity: " + itemId);
                    }
                }
            }
        }
    }

    private static CompoundTag readChunk(Path world, int chunkX, int chunkZ) throws IOException {
        int regionX = Math.floorDiv(chunkX, 32);
        int regionZ = Math.floorDiv(chunkZ, 32);
        Path region = world.resolve("region/r." + regionX + "." + regionZ + ".mca");
        byte[] bytes = Files.readAllBytes(region);
        int localX = Math.floorMod(chunkX, 32);
        int localZ = Math.floorMod(chunkZ, 32);
        int location = ByteBuffer.wrap(bytes, (localX + localZ * 32) * 4, 4).order(ByteOrder.BIG_ENDIAN).getInt();
        require(location != 0, "Documented chunk is absent from " + region.getFileName());
        return readChunkPayload(bytes, (location >>> 8) * 4096);
    }

    private static CompoundTag readChunkPayload(byte[] regionBytes, int offset) throws IOException {
        require(offset >= 8192 && offset + 5 <= regionBytes.length, "Chunk payload offset is invalid");
        int length = ByteBuffer.wrap(regionBytes, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
        require(length > 1 && offset + 4L + length <= regionBytes.length, "Chunk payload length is invalid");
        int compression = regionBytes[offset + 4] & 0x7f;
        require((regionBytes[offset + 4] & 0x80) == 0, "External region chunk streams are prohibited");
        InputStream raw = new ByteArrayInputStream(regionBytes, offset + 5, length - 1);
        InputStream decoded = switch (compression) {
            case 1 -> new GZIPInputStream(raw);
            case 2 -> new InflaterInputStream(raw);
            case 3 -> raw;
            default -> throw new IllegalStateException("Unsupported region compression type " + compression);
        };
        try (DataInputStream input = new DataInputStream(decoded)) {
            return NbtIo.read(input, NbtAccounter.unlimitedHeap());
        }
    }

    private static BlockStateAtPosition blockState(
            CompoundTag chunk, MigrationFixtureManifest.Position position) {
        ListTag sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        int sectionY = Math.floorDiv(position.y(), 16);
        CompoundTag section = null;
        for (int index = 0; index < sections.size(); index++) {
            CompoundTag candidate = sections.getCompound(index);
            if (candidate.getInt("Y") == sectionY) {
                section = candidate;
                break;
            }
        }
        require(section != null, "Documented block section is missing at " + position);
        CompoundTag states = section.getCompound("block_states");
        ListTag palette = states.getList("palette", Tag.TAG_COMPOUND);
        require(!palette.isEmpty(), "Block-state palette is empty at " + position);
        int paletteIndex = 0;
        if (palette.size() > 1) {
            int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            long[] data = states.getLongArray("data");
            int valuesPerLong = 64 / bits;
            int blockIndex = (Math.floorMod(position.y(), 16) << 8)
                    | (Math.floorMod(position.z(), 16) << 4)
                    | Math.floorMod(position.x(), 16);
            int storageIndex = blockIndex / valuesPerLong;
            int bitOffset = (blockIndex % valuesPerLong) * bits;
            require(storageIndex < data.length, "Block-state storage is truncated at " + position);
            paletteIndex = (int) ((data[storageIndex] >>> bitOffset) & ((1L << bits) - 1L));
        }
        require(paletteIndex >= 0 && paletteIndex < palette.size(), "Block-state palette index is invalid");
        CompoundTag value = palette.getCompound(paletteIndex);
        Map<String, String> properties = new LinkedHashMap<>();
        CompoundTag propertyTag = value.getCompound("Properties");
        for (String key : propertyTag.getAllKeys()) {
            properties.put(key, propertyTag.getString(key));
        }
        return new BlockStateAtPosition(value.getString("Name"), Map.copyOf(properties));
    }

    private static CompoundTag blockEntity(CompoundTag chunk, MigrationFixtureManifest.Position position) {
        ListTag entities = chunk.getList("block_entities", Tag.TAG_COMPOUND);
        for (int index = 0; index < entities.size(); index++) {
            CompoundTag candidate = entities.getCompound(index);
            if (candidate.getInt("x") == position.x()
                    && candidate.getInt("y") == position.y()
                    && candidate.getInt("z") == position.z()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Documented Block Entity is missing at " + position);
    }

    private static Map<String, Integer> itemCounts(ListTag items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            CompoundTag item = items.getCompound(index);
            String id = item.getString("id");
            int count = item.getInt("count");
            require(!id.isBlank() && !id.equals("minecraft:air") && count > 0, "Invalid persisted item stack");
            require(!counts.containsKey(id), "Duplicate persisted item stack for " + id);
            counts.put(id, count);
        }
        return Map.copyOf(counts);
    }

    private static CompoundTag readCompressed(Path path) throws IOException {
        require(Files.isRegularFile(path), "Required NBT file is missing: " + path.getFileName());
        try (InputStream input = Files.newInputStream(path)) {
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }

    private static long chunkKey(int x, int z) {
        return ((long) z << 32) | (x & 0xffffffffL);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    record BlockStateAtPosition(String name, Map<String, String> properties) {}

    record StabilizerState(
            MigrationFixtureManifest.Position position,
            String blockId,
            String facing,
            String blockStatus,
            String blockEntityId,
            int schemaVersion,
            String tier,
            String status,
            int cellRemainingTicks,
            int graceRemainingTicks,
            int registeredChunkRadius,
            Map<String, Integer> inventory) {}

    record ContainerState(
            MigrationFixtureManifest.Position position,
            String blockId,
            String blockEntityId,
            Map<String, Integer> items) {}

    record SpawnState(int schemaVersion, boolean initialized, int centerChunkX, int centerChunkZ, long matchingFiles) {}

    record CleanupState(
            int schemaVersion,
            int chunkX,
            int chunkZ,
            int sectionIndex,
            int localBlockIndex,
            boolean completed,
            boolean restartRequired,
            int minSection,
            int sectionCount) {}

    record Snapshot(
            long persistedLevelSeed,
            List<StabilizerState> stabilizers,
            ContainerState container,
            SpawnState spawn,
            CleanupState cleanup) {
        StabilizerState stabilizerAt(MigrationFixtureManifest.Position position) {
            return stabilizers.stream()
                    .filter(state -> Objects.equals(state.position(), position))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Snapshot has no Stabilizer at " + position));
        }
    }
}
