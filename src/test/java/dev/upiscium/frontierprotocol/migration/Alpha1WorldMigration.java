package dev.upiscium.frontierprotocol.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.upiscium.frontierprotocol.migration.MigrationFixtureManifest.CleanupExpectation;
import dev.upiscium.frontierprotocol.migration.MigrationFixtureManifest.ContainerExpectation;
import dev.upiscium.frontierprotocol.migration.MigrationFixtureManifest.Position;
import dev.upiscium.frontierprotocol.migration.MigrationFixtureManifest.Provenance;
import dev.upiscium.frontierprotocol.migration.MigrationFixtureManifest.RuntimeVersions;
import dev.upiscium.frontierprotocol.migration.MigrationFixtureManifest.SpawnExpectation;
import dev.upiscium.frontierprotocol.migration.MigrationFixtureManifest.StabilizerExpectation;
import dev.upiscium.frontierprotocol.startup.ServerProcessRunner;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class Alpha1WorldMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(180);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(60);
    private static final Set<String> REQUIRED_DEPENDENCY_JARS = Set.of(
            "create-1.21.1-6.0.11-295.jar",
            "create-tfmg-1.2.0.jar",
            "fungal-infection-spore-678295-8342823.jar");
    private static final List<Position> STABILIZER_POSITIONS = List.of(
            new Position(20, 100, 84), new Position(22, 100, 84), new Position(24, 100, 84));
    private static final Position CONTAINER_POSITION = new Position(20, 100, 87);
    private static final Map<String, Integer> CONTAINER_ITEMS = Map.of(
            "frontier_protocol:stabilization_compound", 11,
            "frontier_protocol:stabilization_cell", 13,
            "frontier_protocol:tier_1_stabilizer", 1,
            "frontier_protocol:tier_2_stabilizer", 2,
            "frontier_protocol:tier_3_stabilizer", 3);
    private static final Pattern FATAL_LOG = Pattern.compile(
            "(?i)(---- Minecraft Crash Report ----|Mixin (apply|application).*failed|InjectionError|"
                    + "Missing or unsupported mandatory dependencies|Missing mandatory dependenc|"
                    + "unknown.*frontier_protocol|skipping.*frontier_protocol.*block entity|"
                    + "missing registry|failed to load.*(chunk|nbt|saveddata)|\\[[^]]*/ERROR].*DataFixer|"
                    + "unsupported.*schema|corrupt.*(chunk|nbt|world))");
    private static final List<String> PROHIBITED_ARCHIVE_PARTS = List.of(
            "session.lock",
            "playerdata",
            "stats",
            "advancements",
            "logs",
            "crash-reports",
            "mods",
            "libraries",
            "cache");

    private final Arguments arguments;
    private final ServerProcessRunner runner = new ServerProcessRunner();
    private final MigrationWorldInspector inspector = new MigrationWorldInspector();

    private Alpha1WorldMigration(Arguments arguments) {
        this.arguments = arguments;
    }

    public static void main(String[] rawArguments) throws Exception {
        Arguments arguments = Arguments.parse(rawArguments);
        Alpha1WorldMigration migration = new Alpha1WorldMigration(arguments);
        if (arguments.mode().equals("generate")) {
            migration.generateFixture();
        } else if (arguments.mode().equals("verify")) {
            migration.verifyMigration();
        } else {
            throw new IllegalArgumentException("Unknown migration mode: " + arguments.mode());
        }
    }

    private void generateFixture() throws Exception {
        require(arguments.alphaJar() != null && arguments.fixtureBuilderJar() != null,
                "Fixture generation requires the Alpha and helper JARs");
        verifyAlphaJar(arguments.alphaJar());
        Path root = arguments.root().toAbsolutePath().normalize();
        deleteRecursively(root);
        Path server = root.resolve("server");
        prepareServer(server, arguments.alphaJar(), arguments.fixtureBuilderJar(), arguments.classpathArguments());
        ServerProcessRunner.Result first = launch(server, "alpha-first-start.log");
        require(first.output().stream().anyMatch(line -> line.contains("FRONTIER_PROTOCOL_ALPHA1_FIXTURE_BUILDER_COMPLETE")),
                "Alpha fixture builder did not report completion");
        Files.delete(server.resolve("mods").resolve(arguments.fixtureBuilderJar().getFileName()));
        ServerProcessRunner.Result second = launch(server, "alpha-second-start.log");
        verifyConfiguredCapacities(server);
        requireNoFatalMigrationLog(first.output(), "first Alpha start");
        requireNoFatalMigrationLog(second.output(), "second Alpha start");
        require(!Files.exists(server.resolve("crash-reports")), "Alpha fixture generation created a crash report");

        Path output = root.resolve("promotable");
        Files.createDirectories(output);
        Path archive = output.resolve(MigrationFixtureManifest.ARCHIVE_FILENAME);
        createDeterministicArchive(server.resolve("world"), archive);
        String archiveHash = sha256(archive);
        long archiveSize = Files.size(archive);
        require(archiveSize <= MigrationFixtureManifest.ARCHIVE_SIZE_CEILING, "Fixture exceeds size ceiling");

        MigrationFixtureManifest skeleton = skeletonManifest(archiveHash, archiveSize);
        MigrationWorldInspector.Snapshot baseline = inspector.inspect(server.resolve("world"), skeleton);
        MigrationFixtureManifest manifest = manifestFromBaseline(skeleton, baseline);
        assertSnapshot("post-Alpha-restart", baseline, manifest, null);
        Path manifestPath = output.resolve("fixture-manifest.json");
        Files.writeString(manifestPath, GSON.toJson(manifest) + "\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("fixture-manifest.sha256"),
                sha256(manifestPath) + "  fixture-manifest.json\n",
                StandardCharsets.UTF_8);
        verifyArchive(archive, manifest);
        writeGenerationResults(root, manifest, baseline);
    }

    private void verifyMigration() throws Exception {
        Path fixtureDirectory = arguments.fixtureDirectory();
        require(fixtureDirectory != null, "Migration verification requires --fixture-directory");
        Path manifestPath = fixtureDirectory.resolve("fixture-manifest.json");
        Path manifestChecksum = fixtureDirectory.resolve("fixture-manifest.sha256");
        verifyManifestChecksum(manifestPath, manifestChecksum);
        MigrationFixtureManifest manifest = readManifest(manifestPath);
        verifyManifestContract(manifest);
        Path archive = fixtureDirectory.resolve(manifest.archiveFilename());
        verifyArchive(archive, manifest);
        String sourceHashBefore = sha256(archive);

        Path root = arguments.root().toAbsolutePath().normalize();
        deleteRecursively(root);
        Path server = root.resolve("server");
        Files.createDirectories(server);
        extractArchive(archive, server);
        MigrationWorldInspector.Snapshot before = inspector.inspect(server.resolve("world"), manifest);
        assertSnapshot("pre-migration", before, manifest, null);

        prepareServer(server, arguments.productionJar(), null, arguments.classpathArguments());
        ServerProcessRunner.Result firstResult = launch(server, "current-first-start.log");
        verifyConfiguredCapacities(server);
        requireNoFatalMigrationLog(firstResult.output(), "first current-candidate start");
        MigrationWorldInspector.Snapshot first = inspector.inspect(server.resolve("world"), manifest);
        assertSnapshot("first migrated save", first, manifest, before);

        ServerProcessRunner.Result secondResult = launch(server, "current-second-start.log");
        verifyConfiguredCapacities(server);
        requireNoFatalMigrationLog(secondResult.output(), "second current-candidate start");
        MigrationWorldInspector.Snapshot second = inspector.inspect(server.resolve("world"), manifest);
        assertSnapshot("second migrated save", second, manifest, first);
        require(!Files.exists(server.resolve("crash-reports")), "Migration created a crash report");
        require(sourceHashBefore.equals(sha256(archive)), "Committed fixture archive changed during migration");
        writeMigrationResults(root, manifest, before, first, second);
    }

    static void prepareServer(Path server, Path frontierJar, Path additionalMod, Path classpathArguments)
            throws IOException {
        Files.createDirectories(server.resolve("mods"));
        Files.copy(frontierJar, server.resolve("mods").resolve(frontierJar.getFileName()));
        if (additionalMod != null) {
            Files.copy(additionalMod, server.resolve("mods").resolve(additionalMod.getFileName()));
        }
        Files.writeString(server.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);
        Files.writeString(
                server.resolve("server.properties"),
                "level-name=world\nlevel-seed=" + MigrationFixtureManifest.LEVEL_SEED
                        + "\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nview-distance=3\nsimulation-distance=3\n"
                        + "max-world-size=256\nenable-query=false\nspawn-protection=0\n",
                StandardCharsets.UTF_8);
        writeScenarioClasspath(server, classpathArguments);
    }

    private ServerProcessRunner.Result launch(Path server, String logName) throws Exception {
        List<String> command = launchCommand(
                arguments.javaExecutable(), server, arguments.vmArguments(), arguments.programArguments());
        return runner.run(
                command,
                server,
                server.resolve(logName),
                STARTUP_TIMEOUT,
                SHUTDOWN_TIMEOUT,
                ServerProcessRunner.Expectation.SUCCESS);
    }

    static List<String> launchCommand(Path javaExecutable, Path server, Path vmArguments, Path programArguments) {
        return List.of(
                javaExecutable.toString(),
                "-Xmx1G",
                "@" + server.resolve("run-classpath.txt"),
                "@" + vmArguments.toAbsolutePath(),
                "net.neoforged.devlaunch.Main",
                "@" + programArguments.toAbsolutePath());
    }

    private static void writeScenarioClasspath(Path server, Path classpathArguments) throws IOException {
        List<String> source = Files.readAllLines(classpathArguments, StandardCharsets.UTF_8);
        require(source.size() == 2 && source.getFirst().equals("-classpath"), "Unexpected ModDev classpath format");
        List<String> entries = new ArrayList<>(List.of(source.get(1).split(Pattern.quote(java.io.File.pathSeparator))));
        entries.removeIf(entry -> entry.replace('\\', '/').endsWith("/build/classes/java/main")
                || entry.replace('\\', '/').endsWith("/build/resources/main"));
        for (String dependency : REQUIRED_DEPENDENCY_JARS) {
            require(entries.stream().filter(entry -> Path.of(entry).getFileName().toString().equals(dependency)).count() == 1,
                    "Resolved runtime must contain exactly one " + dependency);
        }
        Files.writeString(
                server.resolve("run-classpath.txt"),
                "-classpath\n" + String.join(java.io.File.pathSeparator, entries) + "\n",
                StandardCharsets.UTF_8);
    }

    private static void verifyAlphaJar(Path jar) throws Exception {
        require(jar.getFileName().toString().equals("frontier_protocol-0.1.0-alpha.1.jar"), "Unexpected Alpha JAR filename");
        require(sha256(jar).equals(MigrationFixtureManifest.ALPHA_JAR_SHA256), "Published Alpha JAR checksum differs");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            String metadata;
            try (InputStream input = zip.getInputStream(zip.getEntry("META-INF/neoforge.mods.toml"))) {
                metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            require(metadata.contains("modId=\"frontier_protocol\""), "Alpha metadata lacks Frontier Protocol mod ID");
            require(metadata.contains("version=\"0.1.0-alpha.1\""), "Alpha metadata lacks exact version");
            for (String entry : List.of(
                    "dev/upiscium/frontierprotocol/FrontierProtocolMod.class",
                    "dev/upiscium/frontierprotocol/stabilizer/StabilizerBlockEntity.class",
                    "dev/upiscium/frontierprotocol/mixin/SporeMoundStructureSuppressionMixin.class",
                    "frontier_protocol.mixins.json")) {
                require(zip.getEntry(entry) != null, "Published Alpha JAR is missing " + entry);
            }
        }
    }

    private static MigrationFixtureManifest skeletonManifest(String archiveHash, long archiveSize) {
        List<StabilizerExpectation> stabilizers = List.of(
                new StabilizerExpectation(STABILIZER_POSITIONS.get(0), "frontier_protocol:tier_1_stabilizer",
                        "frontier_protocol:stabilizer", "north", "tier_1", 16, "offline", 0, 0, 0, 8),
                new StabilizerExpectation(STABILIZER_POSITIONS.get(1), "frontier_protocol:tier_2_stabilizer",
                        "frontier_protocol:stabilizer", "east", "tier_2", 4, "grace_period", 0, 0, 1, 32),
                new StabilizerExpectation(STABILIZER_POSITIONS.get(2), "frontier_protocol:tier_3_stabilizer",
                        "frontier_protocol:stabilizer", "south", "tier_3", 12, "grace_period", 0, 0, 2, 64));
        return new MigrationFixtureManifest(
                MigrationFixtureManifest.FIXTURE_SCHEMA_VERSION,
                new Provenance(
                        MigrationFixtureManifest.SOURCE_VERSION,
                        MigrationFixtureManifest.SOURCE_TAG,
                        MigrationFixtureManifest.SOURCE_COMMIT,
                        MigrationFixtureManifest.JAR_URL,
                        MigrationFixtureManifest.CHECKSUM_URL,
                        MigrationFixtureManifest.ALPHA_JAR_SHA256),
                new RuntimeVersions("21", "1.21.1", "21.1.235", "6.0.11-295", "6.0.11", "1.2.0", "2.2.0j", "8342823"),
                MigrationFixtureManifest.LEVEL_SEED,
                MigrationFixtureManifest.ARCHIVE_FILENAME,
                archiveHash,
                archiveSize,
                MigrationFixtureManifest.ARCHIVE_SIZE_CEILING,
                new SpawnExpectation(2, true, 0, 0),
                stabilizers,
                new ContainerExpectation(CONTAINER_POSITION, "minecraft:chest", "minecraft:chest", CONTAINER_ITEMS),
                new CleanupExpectation(1, 7, -3, 2, 321, false, true, -4, 24),
                List.of(
                        "Verify the exact public Alpha JAR and checksum.",
                        "Start Alpha with the generation-only helper, reach Done, stop, and require exit 0.",
                        "Remove the helper and restart the same Alpha world, reach Done, stop, and require exit 0.",
                        "Inspect the post-second-restart world and archive only the sanitized world directory."),
                List.of("session.lock", "logs", "crash reports", "mods", "libraries", "caches", "playerdata", "stats", "advancements"));
    }

    private static MigrationFixtureManifest manifestFromBaseline(
            MigrationFixtureManifest skeleton, MigrationWorldInspector.Snapshot baseline) {
        List<StabilizerExpectation> stabilizers = skeleton.stabilizers().stream().map(expected -> {
            MigrationWorldInspector.StabilizerState actual = baseline.stabilizerAt(expected.position());
            return new StabilizerExpectation(
                    expected.position(),
                    expected.blockId(),
                    expected.blockEntityId(),
                    expected.facing(),
                    expected.tier(),
                    expected.internalCellCount(),
                    actual.status(),
                    actual.cellRemainingTicks(),
                    actual.graceRemainingTicks(),
                    expected.registeredChunkRadius(),
                    expected.configuredCellCapacity());
        }).toList();
        MigrationWorldInspector.SpawnState spawn = baseline.spawn();
        return new MigrationFixtureManifest(
                skeleton.fixtureSchemaVersion(), skeleton.provenance(), skeleton.runtime(), skeleton.levelSeed(),
                skeleton.archiveFilename(), skeleton.archiveSha256(), skeleton.archiveSize(), skeleton.archiveSizeCeiling(),
                new SpawnExpectation(spawn.schemaVersion(), spawn.initialized(), spawn.centerChunkX(), spawn.centerChunkZ()),
                stabilizers, skeleton.container(), skeleton.cleanup(), skeleton.alphaRoundTripProcedure(), skeleton.excludedData());
    }

    static void assertSnapshot(
            String label,
            MigrationWorldInspector.Snapshot snapshot,
            MigrationFixtureManifest manifest,
            MigrationWorldInspector.Snapshot previous) {
        assertSnapshot(label, snapshot, manifest, previous, true);
    }

    static void assertSnapshot(
            String label,
            MigrationWorldInspector.Snapshot snapshot,
            MigrationFixtureManifest manifest,
            MigrationWorldInspector.Snapshot previous,
            boolean requireStatusDiversity) {
        require(snapshot.stabilizers().size() == 3, label + " does not contain all Stabilizers");
        int totalCells = 0;
        Set<Position> positions = new java.util.HashSet<>();
        Set<String> statuses = new java.util.HashSet<>();
        for (StabilizerExpectation expected : manifest.stabilizers()) {
            require(positions.add(expected.position()), "Stabilizer fixture positions are not distinct");
            MigrationWorldInspector.StabilizerState actual = snapshot.stabilizerAt(expected.position());
            require(actual.blockId().equals(expected.blockId()), label + " changed block ID at " + expected.position());
            require(actual.facing().equals(expected.facing()), label + " changed facing at " + expected.position());
            require(actual.blockEntityId().equals(expected.blockEntityId()), label + " changed Block Entity ID");
            require(actual.schemaVersion() == 1 && actual.tier().equals(expected.tier()), label + " changed tier/schema");
            require(actual.blockStatus().equals(actual.status()), label + " BlockState/Block Entity status differs");
            require(Set.of("offline", "active", "grace_period").contains(actual.status()), label + " has unknown status");
            require(actual.cellRemainingTicks() >= 0 && actual.graceRemainingTicks() >= 0, label + " has negative timer");
            require(actual.registeredChunkRadius() == expected.registeredChunkRadius(), label + " changed registered radius");
            require(expected.configuredCellCapacity() == expectedCapacity(expected.tier()),
                    label + " changed configured capacity contract for " + expected.tier());
            int cells = actual.inventory().getOrDefault("frontier_protocol:stabilization_cell", 0);
            require(cells == expected.internalCellCount(), label + " changed internal Cell count");
            require(actual.inventory().size() == 1, label + " contains an unexpected Stabilizer item stack");
            totalCells += cells;
            statuses.add(actual.status());
            if (previous == null) {
                require(actual.status().equals(expected.status()), label + " differs from recorded status");
                require(actual.cellRemainingTicks() == expected.cellRemainingTicks(), label + " differs from recorded Cell timer");
                require(actual.graceRemainingTicks() == expected.graceRemainingTicks(), label + " differs from recorded Grace timer");
            } else {
                MigrationWorldInspector.StabilizerState prior = previous.stabilizerAt(expected.position());
                require(timerMovedMonotonically(prior.cellRemainingTicks(), actual.cellRemainingTicks()),
                        label + " unexpectedly refilled Cell timer");
                require(timerMovedMonotonically(prior.graceRemainingTicks(), actual.graceRemainingTicks()),
                        label + " unexpectedly refilled Grace timer");
            }
        }
        require(totalCells == manifest.stabilizers().stream().mapToInt(StabilizerExpectation::internalCellCount).sum(),
                label + " changed total internal Cell count");
        require(!requireStatusDiversity || statuses.size() >= 2, label + " no longer exercises two statuses");
        require(manifest.stabilizers().stream()
                        .anyMatch(state -> state.internalCellCount() > state.configuredCellCapacity()),
                "Manifest lacks a genuinely over-capacity inventory");

        require(persistedSeedMatches(snapshot.persistedLevelSeed(), manifest.levelSeed()),
                label + " changed the persisted vanilla level seed");

        require(snapshot.container().position().equals(manifest.container().position()), label + " moved container");
        require(snapshot.container().blockId().equals(manifest.container().blockId()), label + " changed container block");
        require(snapshot.container().blockEntityId().equals(manifest.container().blockEntityId()), label + " changed container type");
        require(snapshot.container().items().equals(manifest.container().items()), label + " changed container item counts");

        MigrationWorldInspector.SpawnState spawn = snapshot.spawn();
        require(spawn.schemaVersion() == manifest.spawn().schemaVersion()
                        && spawn.initialized() == manifest.spawn().initialized()
                        && spawn.centerChunkX() == manifest.spawn().centerChunkX()
                        && spawn.centerChunkZ() == manifest.spawn().centerChunkZ()
                        && spawn.matchingFiles() == 1,
                label + " changed initial-spawn SavedData");
        MigrationWorldInspector.CleanupState cleanup = snapshot.cleanup();
        require(cleanup.schemaVersion() == 1 && cleanup.chunkX() == manifest.cleanup().chunkX()
                        && cleanup.chunkZ() == manifest.cleanup().chunkZ(),
                label + " changed cleanup identity/schema");
        require(cleanup.sectionCount() > 0 && cleanup.sectionIndex() >= 0
                        && cleanup.sectionIndex() < cleanup.sectionCount()
                        && cleanup.localBlockIndex() >= 0 && cleanup.localBlockIndex() < 4096,
                label + " has invalid cleanup cursor");
        require(cleanup.minSection() == manifest.cleanup().minSection()
                        && cleanup.sectionCount() == manifest.cleanup().sectionCount(),
                label + " changed cleanup dimension bounds");
        require(cleanupMatchesExpected(cleanup, manifest.cleanup()),
                label + " differs from the exact cleanup baseline");
    }

    static void verifyManifestContract(MigrationFixtureManifest manifest) {
        require(manifest.fixtureSchemaVersion() == MigrationFixtureManifest.FIXTURE_SCHEMA_VERSION, "Unknown fixture schema");
        require(manifest.provenance().equals(new Provenance(
                        MigrationFixtureManifest.SOURCE_VERSION,
                        MigrationFixtureManifest.SOURCE_TAG,
                        MigrationFixtureManifest.SOURCE_COMMIT,
                        MigrationFixtureManifest.JAR_URL,
                        MigrationFixtureManifest.CHECKSUM_URL,
                        MigrationFixtureManifest.ALPHA_JAR_SHA256)),
                "Fixture Alpha provenance differs from the immutable contract");
        require(manifest.runtime().equals(new RuntimeVersions(
                        "21", "1.21.1", "21.1.235", "6.0.11-295", "6.0.11", "1.2.0", "2.2.0j", "8342823")),
                "Fixture runtime versions differ from pinned dependencies");
        require(manifest.levelSeed() == MigrationFixtureManifest.LEVEL_SEED, "Fixture seed differs");
        require(manifest.archiveFilename().equals(MigrationFixtureManifest.ARCHIVE_FILENAME), "Fixture archive name differs");
        require(manifest.archiveSizeCeiling() == MigrationFixtureManifest.ARCHIVE_SIZE_CEILING, "Fixture size ceiling differs");
        require(manifest.archiveSha256().matches("[0-9a-f]{64}"), "Fixture archive hash is malformed");
        require(manifest.provenance().jarSha256().matches("[0-9a-f]{64}"), "Alpha JAR hash is malformed");
        require(manifest.alphaRoundTripProcedure().size() >= 4
                        && manifest.alphaRoundTripProcedure().stream().anyMatch(step -> step.contains("restart")),
                "Fixture does not record the mandatory second Alpha start");
    }

    static MigrationFixtureManifest readManifest(Path manifest) throws IOException {
        return GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), MigrationFixtureManifest.class);
    }

    static void verifyManifestChecksum(Path manifest, Path checksumFile) throws IOException {
        require(Files.isRegularFile(checksumFile), "Fixture manifest checksum is missing");
        String expected = Files.readString(checksumFile, StandardCharsets.UTF_8).trim();
        require(expected.equals(sha256(manifest) + "  fixture-manifest.json"), "Fixture manifest checksum differs");
    }

    static void verifyArchive(Path archive, MigrationFixtureManifest manifest) throws IOException {
        require(Files.isRegularFile(archive), "Fixture archive is missing");
        require(Files.size(archive) == manifest.archiveSize(), "Fixture archive size differs from manifest");
        require(Files.size(archive) <= manifest.archiveSizeCeiling(), "Fixture archive exceeds size ceiling");
        require(sha256(archive).equals(manifest.archiveSha256()), "Fixture archive checksum differs");
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            require(zip.size() > 0, "Fixture archive is empty");
            for (ZipEntry entry : zip.stream().toList()) {
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                require(!name.startsWith("/") && !name.contains("..") && name.startsWith("world/"),
                        "Fixture archive entry is outside the allowlist: " + name);
                require(PROHIBITED_ARCHIVE_PARTS.stream().noneMatch(lower::contains),
                        "Fixture archive contains prohibited data: " + name);
                require(!lower.endsWith(".jar") && !lower.endsWith(".class") && !lower.endsWith(".bin")
                                && !lower.endsWith(".log"),
                        "Fixture archive contains a binary or log: " + name);
                require(!lower.contains("/home/") && !lower.contains("\\users\\"),
                        "Fixture archive contains an absolute local path");
            }
        }
    }

    private static void createDeterministicArchive(Path world, Path archive) throws IOException {
        List<Path> files;
        try (var paths = Files.walk(world)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> allowedWorldFile(world.relativize(path).toString().replace('\\', '/')))
                    .sorted(Comparator.comparing(path -> world.relativize(path).toString()))
                    .toList();
        }
        try (OutputStream raw = Files.newOutputStream(archive); ZipOutputStream zip = new ZipOutputStream(raw)) {
            zip.setLevel(9);
            for (Path file : files) {
                String name = "world/" + world.relativize(file).toString().replace('\\', '/');
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0L);
                zip.putNextEntry(entry);
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private static boolean allowedWorldFile(String relative) {
        String lower = relative.toLowerCase(Locale.ROOT);
        return PROHIBITED_ARCHIVE_PARTS.stream().noneMatch(lower::contains)
                && !lower.endsWith(".jar")
                && !lower.endsWith(".log");
    }

    static void extractArchive(Path archive, Path destination) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            for (ZipEntry entry : zip.stream().toList()) {
                Path output = destination.resolve(entry.getName()).normalize();
                require(output.startsWith(destination), "Fixture archive attempted path traversal");
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    try (InputStream input = zip.getInputStream(entry)) {
                        Files.copy(input, output);
                    }
                }
            }
        }
    }

    static void requireNoFatalMigrationLog(List<String> output, String label) {
        String fatal = output.stream().filter(line -> FATAL_LOG.matcher(line).find()).findFirst().orElse(null);
        require(fatal == null, label + " reported migration failure: " + fatal);
    }

    private static void writeGenerationResults(
            Path root, MigrationFixtureManifest manifest, MigrationWorldInspector.Snapshot baseline) throws IOException {
        Map<String, String> results = commonResults(manifest, baseline, "post-alpha-restart");
        results.put("alpha.first.log", "server/alpha-first-start.log");
        results.put("alpha.second.log", "server/alpha-second-start.log");
        results.put("result", "PASS");
        writeResults(root.resolve("fixture-generation-results.properties"), results, root);
    }

    private static void writeMigrationResults(
            Path root,
            MigrationFixtureManifest manifest,
            MigrationWorldInspector.Snapshot before,
            MigrationWorldInspector.Snapshot first,
            MigrationWorldInspector.Snapshot second) throws IOException {
        Map<String, String> results = commonResults(manifest, before, "pre");
        appendSnapshot(results, first, "first");
        appendSnapshot(results, second, "second");
        results.put("logs", "server/current-first-start.log,server/current-second-start.log");
        results.put("fixture.immutable", "true");
        results.put("result", "PASS");
        writeResults(root.resolve("alpha1-world-upgrade-results.properties"), results, root);
    }

    private static Map<String, String> commonResults(
            MigrationFixtureManifest manifest, MigrationWorldInspector.Snapshot snapshot, String prefix) {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("alpha.jar.sha256", manifest.provenance().jarSha256());
        results.put("fixture.archive.sha256", manifest.archiveSha256());
        results.put("fixture.archive.size", Long.toString(manifest.archiveSize()));
        appendSnapshot(results, snapshot, prefix);
        return results;
    }

    private static void appendSnapshot(
            Map<String, String> results, MigrationWorldInspector.Snapshot snapshot, String prefix) {
        results.put(prefix + ".levelSeed", Long.toString(snapshot.persistedLevelSeed()));
        for (MigrationWorldInspector.StabilizerState state : snapshot.stabilizers()) {
            String tier = state.tier().replace("tier_", "tier");
            results.put(prefix + "." + tier + ".cells",
                    Integer.toString(state.inventory().getOrDefault("frontier_protocol:stabilization_cell", 0)));
            results.put(prefix + "." + tier + ".configuredCapacity",
                    Integer.toString(expectedCapacity(state.tier())));
            results.put(prefix + "." + tier + ".status", state.status());
            results.put(prefix + "." + tier + ".cellTicks", Integer.toString(state.cellRemainingTicks()));
            results.put(prefix + "." + tier + ".graceTicks", Integer.toString(state.graceRemainingTicks()));
        }
        results.put(prefix + ".container.items", snapshot.container().items().toString());
        results.put(prefix + ".spawn.center",
                snapshot.spawn().centerChunkX() + "," + snapshot.spawn().centerChunkZ());
        MigrationWorldInspector.CleanupState cleanup = snapshot.cleanup();
        results.put(prefix + ".cleanup.state",
                cleanup.schemaVersion() + "," + cleanup.chunkX() + "," + cleanup.chunkZ() + ","
                        + cleanup.sectionIndex() + "," + cleanup.localBlockIndex() + "," + cleanup.completed() + ","
                        + cleanup.restartRequired() + "," + cleanup.minSection() + "," + cleanup.sectionCount());
    }

    private static void writeResults(Path output, Map<String, String> results, Path root) throws IOException {
        List<String> lines = results.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        require(lines.stream().noneMatch(line -> line.contains(root.toString())), "Result contains an absolute path");
        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static boolean timerMovedMonotonically(int previous, int current) {
        return previous >= 0 && current >= 0 && current <= previous;
    }

    static boolean persistedSeedMatches(long persistedSeed, long expectedSeed) {
        return persistedSeed == expectedSeed;
    }

    static boolean cleanupMatchesExpected(
            MigrationWorldInspector.CleanupState actual, CleanupExpectation expected) {
        return actual != null
                && actual.schemaVersion() == expected.schemaVersion()
                && actual.chunkX() == expected.chunkX()
                && actual.chunkZ() == expected.chunkZ()
                && actual.sectionIndex() == expected.sectionIndex()
                && actual.localBlockIndex() == expected.localBlockIndex()
                && actual.completed() == expected.completed()
                && actual.restartRequired() == expected.restartRequired()
                && actual.minSection() == expected.minSection()
                && actual.sectionCount() == expected.sectionCount();
    }

    private static int expectedCapacity(String tier) {
        return switch (tier) {
            case "tier_1" -> 8;
            case "tier_2" -> 32;
            case "tier_3" -> 64;
            default -> throw new IllegalStateException("Unknown Stabilizer tier " + tier);
        };
    }

    static void verifyConfiguredCapacities(Path server) throws IOException {
        Path config = server.resolve("config/frontier_protocol-server.toml");
        require(Files.isRegularFile(config), "Frontier Protocol server config was not generated");
        String text = Files.readString(config, StandardCharsets.UTF_8);
        Map<String, Integer> expected = Map.of(
                "tier1CellCapacity", 8,
                "tier2CellCapacity", 32,
                "tier3CellCapacity", 64);
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            var matcher = Pattern.compile(
                            "(?m)^\\s*" + Pattern.quote(entry.getKey()) + "\\s*=\\s*(\\d+)\\s*$")
                    .matcher(text);
            require(matcher.find() && Integer.parseInt(matcher.group(1)) == entry.getValue(),
                    "Configured capacity differs for " + entry.getKey());
        }
    }

    static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) throw exception;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    record Arguments(
            String mode,
            Path root,
            Path javaExecutable,
            Path classpathArguments,
            Path vmArguments,
            Path programArguments,
            Path productionJar,
            Path alphaJar,
            Path fixtureBuilderJar,
            Path fixtureDirectory) {
        static Arguments parse(String[] rawArguments) {
            Map<String, String> values = new HashMap<>();
            for (int index = 0; index < rawArguments.length; index += 2) {
                require(index + 1 < rawArguments.length, "Missing value for " + rawArguments[index]);
                values.put(rawArguments[index], rawArguments[index + 1]);
            }
            for (String key : List.of(
                    "--mode", "--root", "--java", "--classpath-args", "--vm-args", "--program-args", "--production-jar")) {
                require(values.containsKey(key), "Missing required argument " + key);
            }
            return new Arguments(
                    values.get("--mode"),
                    Path.of(values.get("--root")),
                    Path.of(values.get("--java")),
                    Path.of(values.get("--classpath-args")),
                    Path.of(values.get("--vm-args")),
                    Path.of(values.get("--program-args")),
                    Path.of(values.get("--production-jar")),
                    optionalPath(values.get("--alpha-jar")),
                    optionalPath(values.get("--fixture-builder-jar")),
                    optionalPath(values.get("--fixture-directory")));
        }

        private static Path optionalPath(String value) {
            return value == null ? null : Path.of(value);
        }
    }
}
