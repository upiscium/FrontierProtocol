package dev.upiscium.frontierprotocol.startup;

import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionSavedData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

public final class StableStartupMatrix {
    static final String FRONTIER_MOD_ID = "frontier_protocol";
    static final Map<String, String> REQUIRED_DEPENDENCIES = Map.of(
            "create", "create-1.21.1-6.0.11-295.jar",
            "tfmg", "create-tfmg-1.2.0.jar",
            "spore", "fungal-infection-spore-678295-8342823.jar");
    static final List<String> SCENARIO_DIRECTORIES = List.of(
            "fresh-world", "missing-create", "missing-tfmg", "missing-spore", "malformed-config", "out-of-range-config");
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(180);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(60);
    private static final String CONFIG_NAME = "frontier_protocol-server.toml";
    private static final Map<String, NumericRange> OUT_OF_RANGE_VALUES = Map.of(
            "tier1CellCapacity", new NumericRange("0", 1, 64),
            "tier3MinimumRpm", new NumericRange("999", 1, 256),
            "tier1CellDurationTicks", new NumericRange("0", 1, 72_000),
            "spawnProtectionRadiusChunks", new NumericRange("99", 0, 16),
            "cleanupGlobalMutationBudgetPerTick", new NumericRange("0", 1, 4096));

    private final Path root;
    private final Path javaExecutable;
    private final Path classpathArguments;
    private final Path vmArguments;
    private final Path programArguments;
    private final Path productionJar;
    private final ServerProcessRunner runner = new ServerProcessRunner();
    private final Properties results = new Properties();

    private StableStartupMatrix(Arguments arguments) {
        root = arguments.root().toAbsolutePath().normalize();
        javaExecutable = arguments.javaExecutable();
        classpathArguments = arguments.classpathArguments();
        vmArguments = arguments.vmArguments();
        programArguments = arguments.programArguments();
        productionJar = arguments.productionJar();
    }

    public static void main(String[] rawArguments) throws Exception {
        Arguments arguments = Arguments.parse(rawArguments);
        StableStartupMatrix matrix = new StableStartupMatrix(arguments);
        matrix.prepareRoot(arguments.scenario());
        switch (arguments.scenario()) {
            case "fresh" -> matrix.runFreshWorld();
            case "dependencies" -> matrix.runDependencyFailures();
            case "config" -> matrix.runConfigRecovery();
            default -> throw new IllegalArgumentException("Unknown matrix scenario: " + arguments.scenario());
        }
        matrix.writeResults(arguments.scenario());
    }

    private void runFreshWorld() throws Exception {
        Path server = prepareServer("fresh-world", null);
        ServerProcessRunner.Result first = launch(server, "first-start.log", ServerProcessRunner.Expectation.SUCCESS);
        SavedCenter firstCenter = readSavedCenter(server);
        ServerProcessRunner.Result second = launch(server, "second-start.log", ServerProcessRunner.Expectation.SUCCESS);
        SavedCenter secondCenter = readSavedCenter(server);
        require(firstCenter.equals(secondCenter), "Initial-spawn center changed on same-world restart");
        require(countSavedCenterFiles(server) == 1, "Fresh world contains duplicate spawn-protection SavedData files");
        requireNoFatalSavedDataOrMixin(first.output());
        requireNoFatalSavedDataOrMixin(second.output());
        results.setProperty("fresh-world.actual", "Done, stop, exit 0 on both starts; persisted center reused");
        results.setProperty("fresh-world.dimension", "minecraft:overworld");
        results.setProperty("fresh-world.schema", Integer.toString(firstCenter.schemaVersion()));
        results.setProperty("fresh-world.center-chunk-x", Integer.toString(firstCenter.chunkX()));
        results.setProperty("fresh-world.center-chunk-z", Integer.toString(firstCenter.chunkZ()));
        results.setProperty("fresh-world.initialized", Boolean.toString(firstCenter.initialized()));
        results.setProperty("fresh-world.radius-state", "not persisted; supplied by server config");
        results.setProperty("fresh-world.logs", "fresh-world/first-start.log,fresh-world/second-start.log");
    }

    private void runDependencyFailures() throws Exception {
        for (String omitted : REQUIRED_DEPENDENCIES.keySet().stream().sorted().toList()) {
            Path server = prepareServer("missing-" + omitted, omitted);
            ServerProcessRunner.Result result = launch(
                    server, "startup.log", ServerProcessRunner.Expectation.TERMINAL_FAILURE);
            String log = String.join("\n", result.output());
            require(!result.ready(), "Missing " + omitted + " unexpectedly reached Done");
            require(result.exitCode() != 0 || result.terminatedAfterFailure(),
                    "Missing " + omitted + " neither exited nonzero nor was terminated after loader failure");
            require(log.toLowerCase(Locale.ROOT).contains(omitted), "Loader diagnostic does not name " + omitted);
            require(log.toLowerCase(Locale.ROOT).contains(FRONTIER_MOD_ID)
                            || log.contains("Frontier Protocol"),
                    "Loader diagnostic does not identify Frontier Protocol");
            require(Pattern.compile("(?is)(mandatory dependenc|requires|version range|versionrange)").matcher(log).find(),
                    "Loader diagnostic does not identify the dependency requirement");
            require(!Pattern.compile("(?i)(ClassNotFoundException|NoClassDefFoundError|Mixin application failed)")
                            .matcher(log)
                            .find(),
                    "Missing dependency failed after loader validation");
            require(!Files.exists(server.resolve("world/level.dat")), "Missing dependency created a playable world");
            results.setProperty("missing-" + omitted + ".actual", conciseDiagnostic(result.output(), omitted));
            results.setProperty("missing-" + omitted + ".log", "missing-" + omitted + "/startup.log");
        }
    }

    private void runConfigRecovery() throws Exception {
        Path malformedServer = prepareServer("malformed-config", null);
        Path malformedConfig = configPath(malformedServer);
        Files.createDirectories(malformedConfig.getParent());
        Files.writeString(malformedConfig, "spawnProtectionEnabled = [not valid\n", StandardCharsets.UTF_8);
        ServerProcessRunner.Result malformed = launch(
                malformedServer, "invalid-start.log", ServerProcessRunner.Expectation.SUCCESS_OR_TERMINAL_FAILURE);
        if (malformed.ready()) {
            require(!Files.readString(malformedConfig).contains("[not valid"),
                    "NeoForge reached Done without repairing malformed config");
            results.setProperty("malformed-config.actual", "NeoForge repaired the invalid file and reached Done");
            results.setProperty("malformed-config.logs", "malformed-config/invalid-start.log");
        } else {
            String log = String.join("\n", malformed.output());
            require(log.contains(CONFIG_NAME), "Malformed-config failure does not name the config file");
            require(Pattern.compile("(?is)(config|toml).*(parse|invalid|load)|ParsingException").matcher(log).find(),
                    "Malformed-config failure lacks an actionable parse diagnostic");
            Files.delete(malformedConfig);
            ServerProcessRunner.Result recovery = launch(
                    malformedServer, "recovery-start.log", ServerProcessRunner.Expectation.SUCCESS);
            requireNoFatalConfigMixinOrDependency(recovery.output());
            results.setProperty("malformed-config.actual", "Rejected before Done; removing only invalid config recovered");
            results.setProperty(
                    "malformed-config.logs", "malformed-config/invalid-start.log,malformed-config/recovery-start.log");
        }

        Path rangeServer = prepareServer("out-of-range-config", null);
        launch(rangeServer, "bootstrap-start.log", ServerProcessRunner.Expectation.SUCCESS);
        Path rangeConfig = configPath(rangeServer);
        String invalidConfig = Files.readString(rangeConfig, StandardCharsets.UTF_8);
        for (Map.Entry<String, NumericRange> entry : OUT_OF_RANGE_VALUES.entrySet()) {
            invalidConfig = replaceNumericValue(invalidConfig, entry.getKey(), entry.getValue().invalidValue());
        }
        Files.writeString(rangeConfig, invalidConfig, StandardCharsets.UTF_8);
        ServerProcessRunner.Result corrected = launch(
                rangeServer, "corrected-start.log", ServerProcessRunner.Expectation.SUCCESS);
        requireNoFatalConfigMixinOrDependency(corrected.output());
        String correctedConfig = Files.readString(rangeConfig, StandardCharsets.UTF_8);
        for (Map.Entry<String, NumericRange> entry : OUT_OF_RANGE_VALUES.entrySet()) {
            double value = readNumericValue(correctedConfig, entry.getKey());
            require(entry.getValue().contains(value), entry.getKey() + " remained outside its declared safe range: " + value);
            results.setProperty("out-of-range-config." + entry.getKey(), formatNumber(value));
        }
        results.setProperty("out-of-range-config.actual", "NeoForge corrected all representative values into declared ranges");
        results.setProperty(
                "out-of-range-config.logs", "out-of-range-config/bootstrap-start.log,out-of-range-config/corrected-start.log");
    }

    private Path prepareServer(String scenario, String omittedDependency) throws IOException {
        Path server = root.resolve(scenario);
        deleteRecursively(server);
        Files.createDirectories(server.resolve("mods"));
        Files.copy(productionJar, server.resolve("mods").resolve(productionJar.getFileName()));
        Files.writeString(server.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);
        Files.writeString(
                server.resolve("server.properties"),
                "level-name=world\nlevel-seed=8675309\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nenable-query=false\n",
                StandardCharsets.UTF_8);
        writeScenarioClasspath(server, omittedDependency);
        return server;
    }

    private ServerProcessRunner.Result launch(
            Path server, String logName, ServerProcessRunner.Expectation expectation) throws Exception {
        List<String> command = List.of(
                javaExecutable.toString(),
                "-Xmx1G",
                "@" + server.resolve("run-classpath.txt"),
                "@" + vmArguments.toAbsolutePath(),
                "net.neoforged.devlaunch.Main",
                "@" + programArguments.toAbsolutePath());
        return runner.run(command, server, server.resolve(logName), STARTUP_TIMEOUT, SHUTDOWN_TIMEOUT, expectation);
    }

    private void writeScenarioClasspath(Path server, String omittedDependency) throws IOException {
        List<String> source = Files.readAllLines(classpathArguments, StandardCharsets.UTF_8);
        require(source.size() == 2 && source.getFirst().equals("-classpath"), "Unexpected ModDev classpath argument format");
        List<String> entries = new ArrayList<>(List.of(source.get(1).split(Pattern.quote(java.io.File.pathSeparator))));
        entries.removeIf(entry -> entry.equals(root.getParent().resolve("classes/java/main").toString())
                || entry.equals(root.getParent().resolve("resources/main").toString()));
        if (omittedDependency != null) {
            String expectedJar = REQUIRED_DEPENDENCIES.get(omittedDependency);
            long matches = entries.stream().filter(entry -> Path.of(entry).getFileName().toString().equals(expectedJar)).count();
            require(matches == 1, "Expected exactly one resolved " + omittedDependency + " artifact, found " + matches);
            entries.removeIf(entry -> Path.of(entry).getFileName().toString().equals(expectedJar));
        }
        for (Map.Entry<String, String> dependency : REQUIRED_DEPENDENCIES.entrySet()) {
            long matches = entries.stream()
                    .filter(entry -> Path.of(entry).getFileName().toString().equals(dependency.getValue()))
                    .count();
            long expected = dependency.getKey().equals(omittedDependency) ? 0 : 1;
            require(matches == expected,
                    "Scenario " + server.getFileName() + " expected " + expected + " " + dependency.getKey() + " artifact(s)");
        }
        Files.writeString(
                server.resolve("run-classpath.txt"),
                "-classpath\n" + String.join(java.io.File.pathSeparator, entries) + "\n",
                StandardCharsets.UTF_8);
    }

    private SavedCenter readSavedCenter(Path server) throws IOException {
        Path file = server.resolve("world/data").resolve(SpawnProtectionSavedData.DATA_NAME + ".dat");
        require(Files.isRegularFile(file), "Initial-spawn SavedData was not persisted: " + file.getFileName());
        CompoundTag rootTag;
        try (var input = Files.newInputStream(file)) {
            rootTag = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
        CompoundTag data = rootTag.getCompound("data");
        SavedCenter center = new SavedCenter(
                data.getInt("schemaVersion"),
                data.getInt("centerChunkX"),
                data.getInt("centerChunkZ"),
                data.getBoolean("initialized"));
        require(center.schemaVersion() == SpawnProtectionSavedData.SCHEMA_VERSION, "Unexpected spawn SavedData schema");
        require(center.initialized(), "Spawn SavedData remains uninitialized");
        return center;
    }

    private long countSavedCenterFiles(Path server) throws IOException {
        try (var files = Files.list(server.resolve("world/data"))) {
            return files.filter(path -> path.getFileName().toString().startsWith(SpawnProtectionSavedData.DATA_NAME)).count();
        }
    }

    private void writeResults(String scenario) throws IOException {
        results.setProperty("scenario-group", scenario);
        results.setProperty("result", "PASS");
        Path output = root.resolve(scenario + "-results.properties");
        List<String> lines = results.stringPropertyNames().stream()
                .sorted()
                .map(key -> key + "=" + results.getProperty(key))
                .toList();
        require(lines.stream().noneMatch(line -> line.contains(root.toString())), "Result summary contains an absolute local path");
        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    private void prepareRoot(String scenario) throws IOException {
        Files.createDirectories(root);
        switch (scenario) {
            case "fresh" -> deleteRecursively(root.resolve("fresh-world"));
            case "dependencies" -> REQUIRED_DEPENDENCIES.keySet().forEach(name -> uncheckedDelete(root.resolve("missing-" + name)));
            case "config" -> {
                deleteRecursively(root.resolve("malformed-config"));
                deleteRecursively(root.resolve("out-of-range-config"));
            }
            default -> throw new IllegalArgumentException("Unknown matrix scenario: " + scenario);
        }
    }

    private static String conciseDiagnostic(List<String> lines, String dependency) {
        Predicate<String> relevant = line -> {
            String lower = line.toLowerCase(Locale.ROOT);
            return lower.contains(dependency) && lower.contains("requested by: 'frontier_protocol'");
        };
        return lines.stream().filter(relevant).findFirst().map(String::trim).orElse("terminal loader dependency diagnostic");
    }

    static java.util.Set<String> includedDependencies(String omittedDependency) {
        java.util.Set<String> included = new java.util.HashSet<>(REQUIRED_DEPENDENCIES.keySet());
        if (omittedDependency != null) {
            require(included.remove(omittedDependency), "Unknown omitted dependency: " + omittedDependency);
        }
        return java.util.Set.copyOf(included);
    }

    private static String replaceNumericValue(String config, String key, String replacement) {
        Pattern pattern = Pattern.compile("(?m)^(\\s*" + Pattern.quote(key) + "\\s*=\\s*)[-+]?\\d+(?:\\.\\d+)?\\s*$");
        Matcher matcher = pattern.matcher(config);
        require(matcher.find(), "Generated config does not contain " + key);
        return matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + replacement));
    }

    private static double readNumericValue(String config, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*$")
                .matcher(config);
        require(matcher.find(), "Corrected config does not contain " + key);
        return Double.parseDouble(matcher.group(1));
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static Path configPath(Path server) {
        return server.resolve("config").resolve(CONFIG_NAME);
    }

    private static void requireNoFatalSavedDataOrMixin(List<String> output) {
        require(output.stream().noneMatch(line -> Pattern.compile(
                                "(?i)(SavedData.*(fatal|failed)|Mixin (apply|application).*failed|InjectionError|\\[mixin/ERROR])")
                        .matcher(line)
                        .find()),
                "Server log contains fatal SavedData or Mixin output");
    }

    private static void requireNoFatalConfigMixinOrDependency(List<String> output) {
        require(output.stream().noneMatch(line -> Pattern.compile(
                                "(?i)(Failed to load config|ParsingException|Mixin application failed|Missing mandatory dependenc)")
                        .matcher(line)
                        .find()),
                "Recovery log repeats a fatal config, Mixin, or dependency error");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void uncheckedDelete(Path path) {
        try {
            deleteRecursively(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to clean scenario directory " + path.getFileName(), exception);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    record SavedCenter(int schemaVersion, int chunkX, int chunkZ, boolean initialized) {}

    record NumericRange(String invalidValue, double minimum, double maximum) {
        boolean contains(double value) {
            return value >= minimum && value <= maximum;
        }
    }

    record Arguments(
            String scenario,
            Path root,
            Path javaExecutable,
            Path classpathArguments,
            Path vmArguments,
            Path programArguments,
            Path productionJar) {
        static Arguments parse(String[] rawArguments) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int index = 0; index < rawArguments.length; index += 2) {
                require(index + 1 < rawArguments.length, "Missing value for " + rawArguments[index]);
                values.put(rawArguments[index], rawArguments[index + 1]);
            }
            List<String> required = List.of(
                    "--scenario", "--root", "--java", "--classpath-args", "--vm-args", "--program-args", "--production-jar");
            required.forEach(key -> require(values.containsKey(key), "Missing required argument " + key));
            return new Arguments(
                    values.get("--scenario"),
                    Path.of(values.get("--root")),
                    Path.of(values.get("--java")),
                    Path.of(values.get("--classpath-args")),
                    Path.of(values.get("--vm-args")),
                    Path.of(values.get("--program-args")),
                    Path.of(values.get("--production-jar")));
        }
    }
}
