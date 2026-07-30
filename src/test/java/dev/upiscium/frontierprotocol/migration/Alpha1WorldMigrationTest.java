package dev.upiscium.frontierprotocol.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class Alpha1WorldMigrationTest {
    private static final List<String> PROHIBITED = List.of(
            "session.lock", "playerdata", "stats", "advancements", "logs", "crash-reports", "mods", "libraries", "cache");

    @Test
    void committedFixtureHasExactProvenanceAndChecksums() throws Exception {
        Path fixture = fixtureDirectory();
        Path manifestPath = fixture.resolve("fixture-manifest.json");
        MigrationFixtureManifest manifest = readManifest(manifestPath);

        assertEquals(MigrationFixtureManifest.FIXTURE_SCHEMA_VERSION, manifest.fixtureSchemaVersion());
        assertEquals(MigrationFixtureManifest.SOURCE_VERSION, manifest.provenance().sourceModVersion());
        assertEquals(MigrationFixtureManifest.SOURCE_TAG, manifest.provenance().sourceTag());
        assertEquals(MigrationFixtureManifest.SOURCE_COMMIT, manifest.provenance().sourceCommit());
        assertEquals(MigrationFixtureManifest.JAR_URL, manifest.provenance().jarUrl());
        assertEquals(MigrationFixtureManifest.CHECKSUM_URL, manifest.provenance().checksumUrl());
        assertEquals(MigrationFixtureManifest.ALPHA_JAR_SHA256, manifest.provenance().jarSha256());
        assertTrue(manifest.provenance().jarSha256().matches("[0-9a-f]{64}"));

        Path archive = fixture.resolve(manifest.archiveFilename());
        assertEquals(manifest.archiveSha256(), Alpha1WorldMigration.sha256(archive));
        assertEquals(manifest.archiveSize(), Files.size(archive));
        assertTrue(manifest.archiveSize() <= manifest.archiveSizeCeiling());
        String checksum = Files.readString(fixture.resolve("fixture-manifest.sha256"), StandardCharsets.UTF_8).trim();
        assertEquals(Alpha1WorldMigration.sha256(manifestPath) + "  fixture-manifest.json", checksum);
    }

    @Test
    void fixtureArchiveContainsOnlySanitizedWorldData() throws Exception {
        MigrationFixtureManifest manifest = readManifest(fixtureDirectory().resolve("fixture-manifest.json"));
        try (ZipFile zip = new ZipFile(fixtureDirectory().resolve(manifest.archiveFilename()).toFile())) {
            assertFalse(zip.stream().toList().isEmpty());
            zip.stream().forEach(entry -> {
                String name = entry.getName().toLowerCase(java.util.Locale.ROOT);
                assertTrue(name.startsWith("world/"));
                assertFalse(name.contains("..") || name.startsWith("/") || name.contains("/home/") || name.contains("\\users\\"));
                assertTrue(PROHIBITED.stream().noneMatch(name::contains), name);
                assertFalse(name.endsWith(".jar") || name.endsWith(".class") || name.endsWith(".bin") || name.endsWith(".log"));
            });
        }
    }

    @Test
    void fixtureStateContractCoversAllMigrationRisks() throws Exception {
        MigrationFixtureManifest manifest = readManifest(fixtureDirectory().resolve("fixture-manifest.json"));

        assertEquals(3, manifest.stabilizers().size());
        assertEquals(3, new HashSet<>(manifest.stabilizers().stream().map(MigrationFixtureManifest.StabilizerExpectation::position).toList()).size());
        assertEquals(
                List.of(
                        "frontier_protocol:tier_1_stabilizer",
                        "frontier_protocol:tier_2_stabilizer",
                        "frontier_protocol:tier_3_stabilizer"),
                manifest.stabilizers().stream().map(MigrationFixtureManifest.StabilizerExpectation::blockId).toList());
        assertEquals(32, manifest.stabilizers().stream().mapToInt(MigrationFixtureManifest.StabilizerExpectation::internalCellCount).sum());
        MigrationFixtureManifest.StabilizerExpectation tier1 = manifest.stabilizers().getFirst();
        assertEquals(16, tier1.internalCellCount());
        assertEquals(8, tier1.configuredCellCapacity());
        assertTrue(tier1.internalCellCount() > tier1.configuredCellCapacity());
        assertEquals(List.of(8, 32, 64),
                manifest.stabilizers().stream()
                        .map(MigrationFixtureManifest.StabilizerExpectation::configuredCellCapacity)
                        .toList());
        assertTrue(manifest.stabilizers().stream().allMatch(state -> state.cellRemainingTicks() >= 0
                && state.graceRemainingTicks() >= 0
                && state.registeredChunkRadius() >= 0));
        assertTrue(manifest.stabilizers().stream().map(MigrationFixtureManifest.StabilizerExpectation::status).distinct().count() >= 2);
        assertEquals(Map.of(
                        "frontier_protocol:stabilization_compound", 11,
                        "frontier_protocol:stabilization_cell", 13,
                        "frontier_protocol:tier_1_stabilizer", 1,
                        "frontier_protocol:tier_2_stabilizer", 2,
                        "frontier_protocol:tier_3_stabilizer", 3),
                manifest.container().items());
        assertEquals(2, manifest.spawn().schemaVersion());
        assertEquals(1, manifest.cleanup().schemaVersion());
        assertTrue(manifest.alphaRoundTripProcedure().stream().anyMatch(step -> step.contains("restart")));
    }

    @Test
    void generationHelperIsNotPackagedInProductionJar() throws Exception {
        Path productionJar = Path.of(System.getProperty("frontierProtocol.testJar"));
        try (ZipFile zip = new ZipFile(productionJar.toFile())) {
            assertTrue(zip.stream().noneMatch(entry -> entry.getName().contains("fixturebuilder")
                    || entry.getName().contains("frontier_protocol_fixture_builder")));
        }
    }

    @Test
    void timerContractAllowsOnlyNonnegativeNonincreasingValues() {
        assertTrue(Alpha1WorldMigration.timerMovedMonotonically(1000, 999));
        assertTrue(Alpha1WorldMigration.timerMovedMonotonically(1000, 1000));
        assertFalse(Alpha1WorldMigration.timerMovedMonotonically(999, 1000));
        assertFalse(Alpha1WorldMigration.timerMovedMonotonically(1, -1));
    }

    @Test
    void cleanupContractRequiresEveryBaselineFieldExactly() {
        MigrationFixtureManifest.CleanupExpectation expected = new MigrationFixtureManifest.CleanupExpectation(
                1, 7, -3, 2, 321, false, true, -4, 24);
        MigrationWorldInspector.CleanupState baseline = cleanup(1, 2, 321, false, true, -4, 24);

        assertTrue(Alpha1WorldMigration.cleanupMatchesExpected(baseline, expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(cleanup(1, 0, 0, false, true, -4, 24), expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(cleanup(1, 2, 321, false, false, -4, 24), expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(cleanup(1, 2, 321, true, true, -4, 24), expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(cleanup(1, 2, 321, false, true, -3, 24), expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(cleanup(1, 2, 321, false, true, -4, 23), expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(cleanup(2, 2, 321, false, true, -4, 24), expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(
                new MigrationWorldInspector.CleanupState(1, 8, -3, 2, 321, false, true, -4, 24), expected));
        assertFalse(Alpha1WorldMigration.cleanupMatchesExpected(null, expected));
    }

    @Test
    void persistedVanillaSeedMustMatchManifest() {
        assertTrue(Alpha1WorldMigration.persistedSeedMatches(8675309L, 8675309L));
        assertFalse(Alpha1WorldMigration.persistedSeedMatches(8675310L, 8675309L));
    }

    @Test
    void stableLedgerAndSoakRemainIncomplete() throws Exception {
        Path project = Path.of(System.getProperty("frontierProtocol.projectDir"));
        String gates = Files.readString(project.resolve("docs/releases/0.1.0-stable-gates.md"));
        String readiness = Files.readString(project.resolve("docs/stable-readiness-0.1.0.md"));

        assertTrue(gates.contains("| alpha1-world-upgrade | yes | INCOMPLETE |"));
        assertTrue(readiness.lines()
                .filter(line -> line.contains("Normal-operation log volume"))
                .allMatch(line -> line.contains("NOT VERIFIED")));
    }

    private static MigrationFixtureManifest readManifest(Path path) throws Exception {
        return new Gson().fromJson(Files.readString(path, StandardCharsets.UTF_8), MigrationFixtureManifest.class);
    }

    private static MigrationWorldInspector.CleanupState cleanup(
            int schema,
            int sectionIndex,
            int localBlockIndex,
            boolean completed,
            boolean restartRequired,
            int minSection,
            int sectionCount) {
        return new MigrationWorldInspector.CleanupState(
                schema, 7, -3, sectionIndex, localBlockIndex, completed, restartRequired, minSection, sectionCount);
    }

    private static Path fixtureDirectory() {
        return Path.of(System.getProperty("frontierProtocol.projectDir"))
                .resolve("src/test/resources/migration/0.1.0-alpha.1");
    }
}
