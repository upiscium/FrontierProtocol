package dev.upiscium.frontierprotocol.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.upiscium.frontierprotocol.startup.ServerProcessRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ReleaseCandidateSoak {
    static final long MINIMUM_EVIDENCE_MINUTES = 120;
    static final long MAXIMUM_LOG_BYTES = 10L * 1024L * 1024L;
    static final int MAXIMUM_NORMALIZED_DUPLICATES = 3;
    static final double MAXIMUM_FRONTIER_LINES_PER_MINUTE = 0.25;
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofMinutes(1);
    private static final Pattern FRONTIER_LINE = Pattern.compile("(?i)(frontier_protocol|Frontier Protocol)");
    private static final Pattern WARNING_LINE = Pattern.compile("(?i)\\[[^]]*/WARN]");
    private static final Pattern ERROR_LINE = Pattern.compile("(?i)\\[[^]]*/ERROR]");
    private static final Pattern FATAL_LINE = Pattern.compile("(?i)(\\[[^]]*/FATAL]|---- Minecraft Crash Report ----)");
    private static final Pattern RELEVANT_ERROR = Pattern.compile(
            "(?i)(frontier_protocol|Frontier Protocol|registry|NBT|SavedData|Mixin|chunk|DataFixer)");
    private static final Set<String> WARNING_ALLOWLIST = Set.of();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Arguments arguments;
    private final MigrationWorldInspector inspector = new MigrationWorldInspector();
    private final ServerProcessRunner runner = new ServerProcessRunner();

    private ReleaseCandidateSoak(Arguments arguments) {
        this.arguments = arguments;
    }

    public static void main(String[] rawArguments) throws Exception {
        Arguments arguments = Arguments.parse(rawArguments);
        ReleaseCandidateSoak soak = new ReleaseCandidateSoak(arguments);
        try {
            soak.run();
        } catch (Throwable failure) {
            soak.writeFailure(failure);
            throw failure;
        }
    }

    private void run() throws Exception {
        validateDuration(arguments.durationMinutes(), arguments.allowShortSoak());
        require(arguments.sourceCommit().matches("[0-9a-fA-F]{40}"), "Source commit must be a full SHA");
        Path root = arguments.root().toAbsolutePath().normalize();
        Alpha1WorldMigration.deleteRecursively(root);
        Files.createDirectories(root);

        Path manifestPath = arguments.fixtureDirectory().resolve("fixture-manifest.json");
        Alpha1WorldMigration.verifyManifestChecksum(
                manifestPath, arguments.fixtureDirectory().resolve("fixture-manifest.sha256"));
        MigrationFixtureManifest manifest = Alpha1WorldMigration.readManifest(manifestPath);
        Alpha1WorldMigration.verifyManifestContract(manifest);
        Path archive = arguments.fixtureDirectory().resolve(manifest.archiveFilename());
        Alpha1WorldMigration.verifyArchive(archive, manifest);
        String fixtureHash = Alpha1WorldMigration.sha256(archive);

        Path server = root.resolve("server");
        Files.createDirectories(server);
        Alpha1WorldMigration.extractArchive(archive, server);
        MigrationWorldInspector.Snapshot before = inspector.inspect(server.resolve("world"), manifest);
        Alpha1WorldMigration.assertSnapshot("pre-soak", before, manifest, null);
        Alpha1WorldMigration.prepareServer(
                server, arguments.productionJar(), null, arguments.classpathArguments());

        Duration warmup = Duration.ofSeconds(arguments.warmupSeconds());
        Duration steadyState = Duration.ofMinutes(arguments.durationMinutes());
        Path log = server.resolve("rc-soak.log");
        ServerProcessRunner.Result process = runner.run(
                Alpha1WorldMigration.launchCommand(
                        arguments.javaExecutable(), server, arguments.vmArguments(), arguments.programArguments()),
                server,
                log,
                STARTUP_TIMEOUT,
                warmup,
                steadyState,
                SHUTDOWN_TIMEOUT,
                ServerProcessRunner.Expectation.SUCCESS);

        require(process.ready(), "Server did not reach canonical Done");
        require(process.stopped(), "Server did not acknowledge normal shutdown");
        require(process.exitCode() == 0, "Server exited with nonzero status");
        require(process.measuredSteadyState().compareTo(steadyState) >= 0,
                "Measured steady-state duration was shorter than requested");
        require(!Files.exists(server.resolve("crash-reports")), "Soak created a crash report");
        Alpha1WorldMigration.verifyConfiguredCapacities(server);
        verifyNormalConfig(server.resolve("config/frontier_protocol-server.toml"));

        MigrationWorldInspector.Snapshot after = inspector.inspect(server.resolve("world"), manifest);
        Alpha1WorldMigration.assertSnapshot("post-soak", after, manifest, before, false);
        require(before.container().items().equals(after.container().items()), "Soak changed container item counts");
        require(before.spawn().equals(after.spawn()), "Soak changed spawn SavedData");
        require(before.cleanup().equals(after.cleanup()), "Soak changed cleanup SavedData");
        require(before.persistedLevelSeed() == after.persistedLevelSeed(), "Soak changed persisted seed");
        require(fixtureHash.equals(Alpha1WorldMigration.sha256(archive)), "Soak changed fixture archive bytes");

        LogMetrics metrics = analyzeLog(process, log, steadyState);
        Map<String, Object> results = baseResults(fixtureHash);
        results.put("measuredWarmupSeconds", process.measuredWarmup().toMillis() / 1000.0);
        results.put("measuredSteadyStateSeconds", process.measuredSteadyState().toMillis() / 1000.0);
        results.put("done", process.ready());
        results.put("shutdownAcknowledged", process.stopped());
        results.put("exitStatus", process.exitCode());
        results.put("totalLogBytes", metrics.totalBytes());
        results.put("totalLogLines", metrics.totalLines());
        results.put("frontierProtocolLineCount", metrics.frontierLines());
        results.put("frontierProtocolLinesPerMinute", metrics.frontierRate());
        results.put("warningCount", metrics.warningCount());
        results.put("errorCount", metrics.errorCount());
        results.put("fatalCount", metrics.fatalCount());
        results.put("normalizedMessageFrequency", metrics.normalizedFrequency());
        results.put("persistedSeed", after.persistedLevelSeed());
        results.put("preStabilizers", stabilizerSummary(before));
        results.put("postStabilizers", stabilizerSummary(after));
        results.put("preItemCounts", itemSummary(before));
        results.put("postItemCounts", itemSummary(after));
        results.put("spawn", after.spawn());
        results.put("cleanup", after.cleanup());
        results.put("result", "PASS");
        writeResults(results);
    }

    static void validateDuration(long durationMinutes, boolean allowShortSoak) {
        if (durationMinutes <= 0) throw new IllegalArgumentException("Soak duration must be positive");
        if (durationMinutes < MINIMUM_EVIDENCE_MINUTES && !allowShortSoak) {
            throw new IllegalArgumentException(
                    "Evidence soak requires at least 120 minutes; short mode requires allowShortSoak=true");
        }
    }

    static String normalizeMessage(String line) {
        return line.replaceFirst("^\\[[0-9:]+] \\[[^]]+] \\[[^]]+][: ]*", "")
                .replaceAll("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b", "<uuid>")
                .replaceAll("\\((-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+)\\)", "(<coord>)")
                .replaceAll("(?<![A-Za-z0-9_.])-?\\d+(?![A-Za-z0-9_.])", "<n>")
                .trim();
    }

    static int scaledFrontierLineLimit(long durationMinutes) {
        return Math.max(1, (int) Math.ceil(30.0 * durationMinutes / MINIMUM_EVIDENCE_MINUTES));
    }

    static void requireLogThresholds(
            long totalBytes,
            int frontierLines,
            double frontierRate,
            int repeatedWarnings,
            int relevantErrors,
            int fatalCount,
            Map<String, Integer> normalizedFrequency,
            long durationMinutes) {
        require(totalBytes <= MAXIMUM_LOG_BYTES, "Server log exceeded 10 MiB");
        require(fatalCount == 0, "Server log contains a fatal condition");
        require(relevantErrors == 0, "Server log contains a relevant error");
        require(repeatedWarnings == 0, "Server log contains a repeated unallowlisted Frontier Protocol warning");
        require(frontierLines <= scaledFrontierLineLimit(durationMinutes), "Frontier Protocol line count exceeded limit");
        require(frontierRate <= MAXIMUM_FRONTIER_LINES_PER_MINUTE, "Frontier Protocol log rate exceeded limit");
        require(normalizedFrequency.values().stream().noneMatch(count -> count > MAXIMUM_NORMALIZED_DUPLICATES),
                "A normalized Frontier Protocol message exceeded the duplicate limit");
    }

    static boolean containsAbsolutePath(String json) {
        return Pattern.compile("(?m)(?:[A-Za-z]:\\\\|/(?:home|Users|tmp|var|opt)/)").matcher(json).find();
    }

    private LogMetrics analyzeLog(
            ServerProcessRunner.Result process, Path log, Duration requestedSteadyState) throws IOException {
        long totalBytes = Files.size(log);
        int totalLines = process.output().size();
        int warningCount = 0;
        int errorCount = 0;
        int fatalCount = 0;
        int relevantErrors = 0;
        for (String line : process.output()) {
            if (WARNING_LINE.matcher(line).find()) warningCount++;
            if (ERROR_LINE.matcher(line).find()) {
                errorCount++;
                if (RELEVANT_ERROR.matcher(line).find()) relevantErrors++;
            }
            if (FATAL_LINE.matcher(line).find()) fatalCount++;
        }

        List<String> steadyFrontier = process.timedOutput().stream()
                .filter(line -> line.observedAtNanos() >= process.steadyStateStartedAtNanos())
                .map(ServerProcessRunner.TimedLine::text)
                .filter(line -> FRONTIER_LINE.matcher(line).find())
                .toList();
        Map<String, Integer> normalized = new LinkedHashMap<>();
        Map<String, Integer> warningFrequency = new LinkedHashMap<>();
        for (String line : steadyFrontier) {
            String message = normalizeMessage(line);
            normalized.merge(message, 1, Integer::sum);
            if (WARNING_LINE.matcher(line).find() && !WARNING_ALLOWLIST.contains(message)) {
                warningFrequency.merge(message, 1, Integer::sum);
            }
        }
        int repeatedWarnings = warningFrequency.values().stream().mapToInt(count -> count > 1 ? 1 : 0).sum();
        double measuredMinutes = Math.max(process.measuredSteadyState().toMillis() / 60000.0, 1.0 / 60000.0);
        double rate = steadyFrontier.size() / measuredMinutes;
        requireLogThresholds(totalBytes, steadyFrontier.size(), rate, repeatedWarnings, relevantErrors,
                fatalCount, normalized, requestedSteadyState.toMinutes());
        return new LogMetrics(totalBytes, totalLines, steadyFrontier.size(), rate, warningCount, errorCount,
                fatalCount, Map.copyOf(normalized));
    }

    private static void verifyNormalConfig(Path config) throws IOException {
        String text = Files.readString(config, StandardCharsets.UTF_8);
        require(configValue(text, "debugLogging").equals("false"), "debugLogging must remain false");
        require(configValue(text, "tier1CellCapacity").equals("8"), "Tier 1 capacity changed");
        require(configValue(text, "tier2CellCapacity").equals("32"), "Tier 2 capacity changed");
        require(configValue(text, "tier3CellCapacity").equals("64"), "Tier 3 capacity changed");
    }

    private static String configValue(String text, String key) {
        var matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*([^#\\s]+)\\s*$").matcher(text);
        require(matcher.find(), "Missing server config value " + key);
        return matcher.group(1).toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> baseResults(String fixtureHash) {
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("modVersion", arguments.modVersion());
        results.put("sourceCommit", arguments.sourceCommit());
        results.put("fixtureArchiveSha256", fixtureHash);
        results.put("requestedDurationMinutes", arguments.durationMinutes());
        results.put("shortHarnessMode", arguments.allowShortSoak());
        results.put("logPath", "server/rc-soak.log");
        results.put("measuredWarmupSeconds", 0);
        results.put("measuredSteadyStateSeconds", 0);
        results.put("done", false);
        results.put("shutdownAcknowledged", false);
        results.put("exitStatus", "unavailable");
        results.put("totalLogBytes", 0);
        results.put("totalLogLines", 0);
        results.put("frontierProtocolLineCount", 0);
        results.put("frontierProtocolLinesPerMinute", 0);
        results.put("warningCount", 0);
        results.put("errorCount", 0);
        results.put("fatalCount", 0);
        results.put("normalizedMessageFrequency", Map.of());
        results.put("persistedSeed", "unavailable");
        results.put("preStabilizers", List.of());
        results.put("postStabilizers", List.of());
        results.put("preItemCounts", Map.of());
        results.put("postItemCounts", Map.of());
        results.put("spawn", "unavailable");
        results.put("cleanup", "unavailable");
        return results;
    }

    private static List<Map<String, Object>> stabilizerSummary(MigrationWorldInspector.Snapshot snapshot) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MigrationWorldInspector.StabilizerState state : snapshot.stabilizers()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("position", state.position());
            entry.put("blockId", state.blockId());
            entry.put("facing", state.facing());
            entry.put("blockEntityId", state.blockEntityId());
            entry.put("tier", state.tier());
            entry.put("status", state.status());
            entry.put("cellCount", state.inventory().getOrDefault("frontier_protocol:stabilization_cell", 0));
            result.add(entry);
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> itemSummary(MigrationWorldInspector.Snapshot snapshot) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stabilizers", stabilizerSummary(snapshot));
        result.put("container", snapshot.container().items());
        return result;
    }

    private void writeFailure(Throwable failure) {
        try {
            Map<String, Object> results = baseResults("unavailable");
            results.put("result", "FAIL");
            results.put("failureReason", failure.getClass().getSimpleName() + ": " + sanitize(failure.getMessage()));
            writeResults(results);
        } catch (Exception ignored) {
            // Preserve the original failure when evidence writing itself is unavailable.
        }
    }

    private String sanitize(String message) {
        if (message == null) return "No message";
        String result = message;
        for (Path path : List.of(arguments.root(), arguments.fixtureDirectory(), arguments.productionJar(),
                arguments.classpathArguments(), arguments.vmArguments(), arguments.programArguments())) {
            result = result.replace(path.toAbsolutePath().normalize().toString(), path.getFileName().toString());
        }
        return result;
    }

    private void writeResults(Map<String, Object> results) throws IOException {
        Path output = arguments.root().resolve("rc-soak-results.json");
        Files.createDirectories(output.getParent());
        String json = GSON.toJson(results) + "\n";
        require(!containsAbsolutePath(json), "RC soak result contains an absolute path");
        Files.writeString(output, json, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record LogMetrics(
            long totalBytes,
            int totalLines,
            int frontierLines,
            double frontierRate,
            int warningCount,
            int errorCount,
            int fatalCount,
            Map<String, Integer> normalizedFrequency) {}

    private record Arguments(
            Path root,
            Path fixtureDirectory,
            Path productionJar,
            Path javaExecutable,
            Path classpathArguments,
            Path vmArguments,
            Path programArguments,
            String modVersion,
            String sourceCommit,
            long durationMinutes,
            long warmupSeconds,
            boolean allowShortSoak) {
        static Arguments parse(String[] raw) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int index = 0; index < raw.length; index += 2) {
                if (index + 1 >= raw.length || !raw[index].startsWith("--")) {
                    throw new IllegalArgumentException("Arguments must use --name value pairs");
                }
                values.put(raw[index].substring(2), raw[index + 1]);
            }
            return new Arguments(
                    path(values, "root"),
                    path(values, "fixture-directory"),
                    path(values, "production-jar"),
                    path(values, "java"),
                    path(values, "classpath-args"),
                    path(values, "vm-args"),
                    path(values, "program-args"),
                    required(values, "mod-version"),
                    required(values, "source-commit"),
                    Long.parseLong(required(values, "duration-minutes")),
                    Long.parseLong(required(values, "warmup-seconds")),
                    Boolean.parseBoolean(required(values, "allow-short-soak")));
        }

        private static Path path(Map<String, String> values, String key) {
            return Path.of(required(values, key));
        }

        private static String required(Map<String, String> values, String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing --" + key);
            return value;
        }
    }
}
