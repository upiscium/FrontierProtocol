package dev.upiscium.frontierprotocol.migration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.startup.ServerProcessRunner;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseCandidateSoakTest {
    @TempDir
    Path tempDirectory;

    @Test
    void evidenceDurationRequiresAtLeastOneHundredTwentyMinutes() {
        assertDoesNotThrow(() -> ReleaseCandidateSoak.validateDuration(120, false));
        assertDoesNotThrow(() -> ReleaseCandidateSoak.validateDuration(2, true));
        assertThrows(IllegalArgumentException.class, () -> ReleaseCandidateSoak.validateDuration(2, false));
        assertThrows(IllegalArgumentException.class, () -> ReleaseCandidateSoak.validateDuration(0, true));
    }

    @Test
    void normalizationRemovesOnlyKnownVolatileFields() {
        String first = ReleaseCandidateSoak.normalizeMessage(
                "[12:00:00] [Server thread/INFO] [frontier_protocol/]: Cleanup at (1, 2, 3), count 40");
        String second = ReleaseCandidateSoak.normalizeMessage(
                "[12:01:00] [Worker/INFO] [frontier_protocol/]: Cleanup at (4, 5, 6), count 41");
        assertEquals(first, second);
        assertFalse(first.contains("12:00"));
        assertTrue(first.contains("Cleanup"));
    }

    @Test
    void logThresholdsRejectDuplicatesRateSizeErrorsAndFatals() {
        Map<String, Integer> valid = Map.of("quiet", 3);
        assertDoesNotThrow(() -> ReleaseCandidateSoak.requireLogThresholds(1024, 0, 0, 0, 0, 0, valid, 120));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(1024, 0, 0, 0, 0, 0, Map.of("spam", 4), 120));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(1024, 31, 0.25, 0, 0, 0, valid, 120));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(1024, 1, 0.26, 0, 0, 0, valid, 120));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(ReleaseCandidateSoak.MAXIMUM_LOG_BYTES + 1,
                        0, 0, 0, 0, 0, valid, 120));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(1024, 0, 0, 1, 0, 0, valid, 120));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(1024, 0, 0, 0, 1, 0, valid, 120));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(1024, 0, 0, 0, 0, 1, valid, 120));
    }

    @Test
    void shortModeScalesAbsoluteLineLimitButNotDuplicateLimit() {
        assertEquals(30, ReleaseCandidateSoak.scaledFrontierLineLimit(120));
        assertEquals(1, ReleaseCandidateSoak.scaledFrontierLineLimit(2));
        assertThrows(IllegalStateException.class,
                () -> ReleaseCandidateSoak.requireLogThresholds(1024, 0, 0, 0, 0, 0, Map.of("spam", 4), 2));
    }

    @Test
    void resultPathSafeguardRejectsAbsolutePaths() {
        assertTrue(ReleaseCandidateSoak.containsAbsolutePath("{\"path\":\"/home/runner/work/file\"}"));
        assertTrue(ReleaseCandidateSoak.containsAbsolutePath("{\"path\":\"C:\\\\runner\\\\file\"}"));
        assertFalse(ReleaseCandidateSoak.containsAbsolutePath("{\"path\":\"server/rc-soak.log\"}"));
    }

    @Test
    void observationRequiresLivenessAndNormalStop() throws Exception {
        ServerProcessRunner runner = new ServerProcessRunner();
        String ready = "Done (0.100s)! For help, type \"help\"";
        ServerProcessRunner.Result result = runner.run(
                List.of("/bin/sh", "-c", "printf '%s\\n' '" + ready
                        + "'; read command; test \"$command\" = stop; printf 'Stopping server\\n'"),
                tempDirectory.resolve("success"),
                tempDirectory.resolve("success.log"),
                Duration.ofSeconds(2),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofSeconds(2),
                ServerProcessRunner.Expectation.SUCCESS);
        assertTrue(result.ready());
        assertTrue(result.stopped());
        assertEquals(0, result.exitCode());
        assertTrue(result.measuredWarmup().toMillis() >= 50);
        assertTrue(result.measuredSteadyState().toMillis() >= 100);

        assertThrows(IllegalStateException.class, () -> runner.run(
                List.of("/bin/sh", "-c", "printf '%s\\n' '" + ready + "'; sleep 0.1; exit 7"),
                tempDirectory.resolve("early-exit"),
                tempDirectory.resolve("early-exit.log"),
                Duration.ofSeconds(2),
                Duration.ZERO,
                Duration.ofSeconds(2),
                Duration.ofSeconds(1),
                ServerProcessRunner.Expectation.SUCCESS));
    }
}
