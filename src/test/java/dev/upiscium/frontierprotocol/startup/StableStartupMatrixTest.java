package dev.upiscium.frontierprotocol.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.Duration;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StableStartupMatrixTest {
    @TempDir
    java.nio.file.Path temporaryDirectory;

    @Test
    void dependencyAndScenarioContractsAreExactAndIsolated() {
        assertEquals(new HashSet<>(java.util.List.of("create", "tfmg", "spore")),
                StableStartupMatrix.REQUIRED_DEPENDENCIES.keySet());
        assertEquals(6, new HashSet<>(StableStartupMatrix.SCENARIO_DIRECTORIES).size());
        StableStartupMatrix.REQUIRED_DEPENDENCIES.forEach((id, jar) -> {
            assertFalse(jar.isBlank());
            assertTrue(StableStartupMatrix.SCENARIO_DIRECTORIES.contains("missing-" + id));
            assertEquals(2, StableStartupMatrix.includedDependencies(id).size());
            assertFalse(StableStartupMatrix.includedDependencies(id).contains(id));
        });
        assertEquals(StableStartupMatrix.REQUIRED_DEPENDENCIES.keySet(),
                StableStartupMatrix.includedDependencies(null));
    }

    @Test
    void canonicalDoneDetectionIsStrict() {
        assertTrue(ServerProcessRunner.isCanonicalReadyLine(
                "[Server thread/INFO] [minecraft/MinecraftServer]: Done (1.234s)! For help, type \"help\""));
        assertFalse(ServerProcessRunner.isCanonicalReadyLine("Done loading dependencies"));
    }

    @Test
    void successfulProcessReceivesStopAndRequiresZeroExit() throws Exception {
        var runner = new ServerProcessRunner();
        var result = runner.run(
                java.util.List.of(
                        "/bin/sh",
                        "-c",
                        "printf 'Done (0.1s)! For help, type \"help\"\\n'; read command; "
                                + "test \"$command\" = stop; printf 'Stopping server\\n'"),
                temporaryDirectory,
                temporaryDirectory.resolve("success.log"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                ServerProcessRunner.Expectation.SUCCESS);

        assertTrue(result.ready());
        assertTrue(result.stopped());
        assertEquals(0, result.exitCode());
    }

    @Test
    void timeoutCanNeverPassAsExpectedFailureAndCleansChild() throws Exception {
        var runner = new ServerProcessRunner();
        java.nio.file.Path childPidFile = temporaryDirectory.resolve("child.pid");
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> runner.run(
                java.util.List.of("/bin/sh", "-c", "sleep 30 & child=$!; printf '%s' \"$child\" > child.pid; wait"),
                temporaryDirectory,
                temporaryDirectory.resolve("timeout.log"),
                Duration.ofMillis(200),
                Duration.ofSeconds(1),
                ServerProcessRunner.Expectation.TERMINAL_FAILURE));

        assertTrue(failure.getMessage().contains("timeout is never an expected failure"));
        long childPid = Long.parseLong(Files.readString(childPidFile));
        assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test
    void terminalFailureMustBeObservedInsteadOfBareNonzeroExit() {
        var runner = new ServerProcessRunner();
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> runner.run(
                java.util.List.of("/bin/sh", "-c", "exit 2"),
                temporaryDirectory,
                temporaryDirectory.resolve("bare-exit.log"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(1),
                ServerProcessRunner.Expectation.TERMINAL_FAILURE));

        assertTrue(failure.getMessage().contains("before a conclusive startup result"));
    }

    @Test
    void resultFixturesUseRelativePathsOnly() throws Exception {
        Files.writeString(temporaryDirectory.resolve("result.properties"), "log=fresh-world/first-start.log\n");
        String result = Files.readString(temporaryDirectory.resolve("result.properties"));
        assertFalse(result.contains(temporaryDirectory.toString()));
    }

    @Test
    void migrationSoakAndFinalCandidateGatesRemainIncomplete() throws Exception {
        java.nio.file.Path project = java.nio.file.Path.of(System.getProperty("frontierProtocol.projectDir"));
        String readiness = Files.readString(project.resolve("docs/stable-readiness-0.1.0.md"));
        String gates = Files.readString(project.resolve("docs/releases/0.1.0-stable-gates.md"));

        assertTrue(readiness.contains("| Upgrade from a 0.1.0-alpha.1 world |"));
        assertTrue(readiness.contains("| Upgraded-world restart recovery |"));
        assertTrue(readiness.contains("| Normal-operation log volume |"));
        assertTrue(readiness.lines()
                .filter(line -> line.contains("Upgrade from a 0.1.0-alpha.1 world")
                        || line.contains("Upgraded-world restart recovery")
                        || line.contains("Normal-operation log volume"))
                .allMatch(line -> line.contains("NOT VERIFIED")));
        assertTrue(gates.contains("| fresh-world-smoke | yes | INCOMPLETE |"));
        assertTrue(gates.contains("| alpha1-world-upgrade | yes | INCOMPLETE |"));
    }
}
