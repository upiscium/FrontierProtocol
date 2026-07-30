package dev.upiscium.frontierprotocol.migration;

import java.util.List;
import java.util.Map;

record MigrationFixtureManifest(
        int fixtureSchemaVersion,
        Provenance provenance,
        RuntimeVersions runtime,
        long levelSeed,
        String archiveFilename,
        String archiveSha256,
        long archiveSize,
        long archiveSizeCeiling,
        SpawnExpectation spawn,
        List<StabilizerExpectation> stabilizers,
        ContainerExpectation container,
        CleanupExpectation cleanup,
        List<String> alphaRoundTripProcedure,
        List<String> excludedData) {
    static final int FIXTURE_SCHEMA_VERSION = 2;
    static final String SOURCE_VERSION = "0.1.0-alpha.1";
    static final String SOURCE_TAG = "v0.1.0-alpha.1";
    static final String SOURCE_COMMIT = "fed467ec0cd52a936f06751cd922efcc259914a1";
    static final String JAR_URL =
            "https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0-alpha.1/frontier_protocol-0.1.0-alpha.1.jar";
    static final String CHECKSUM_URL = JAR_URL + ".sha256";
    static final String ALPHA_JAR_SHA256 = "60cb68f625eab26e8a37fe86eed125c52a1c00efca5dcae85ff3db5085274a8f";
    static final String ARCHIVE_FILENAME = "world-fixture.zip";
    static final long ARCHIVE_SIZE_CEILING = 16L * 1024L * 1024L;
    static final long LEVEL_SEED = 8675309L;

    record Provenance(
            String sourceModVersion,
            String sourceTag,
            String sourceCommit,
            String jarUrl,
            String checksumUrl,
            String jarSha256) {}

    record RuntimeVersions(
            String java,
            String minecraft,
            String neoForge,
            String createArtifact,
            String createMod,
            String tfmg,
            String spore,
            String sporeFileId) {}

    record Position(int x, int y, int z) {
        int chunkX() {
            return Math.floorDiv(x, 16);
        }

        int chunkZ() {
            return Math.floorDiv(z, 16);
        }
    }

    record SpawnExpectation(int schemaVersion, boolean initialized, int centerChunkX, int centerChunkZ) {}

    record StabilizerExpectation(
            Position position,
            String blockId,
            String blockEntityId,
            String facing,
            String tier,
            int internalCellCount,
            String status,
            int cellRemainingTicks,
            int graceRemainingTicks,
            int registeredChunkRadius,
            int configuredCellCapacity) {}

    record ContainerExpectation(Position position, String blockId, String blockEntityId, Map<String, Integer> items) {}

    record CleanupExpectation(
            int schemaVersion,
            int chunkX,
            int chunkZ,
            int sectionIndex,
            int localBlockIndex,
            boolean completed,
            boolean restartRequired,
            int minSection,
            int sectionCount) {}
}
