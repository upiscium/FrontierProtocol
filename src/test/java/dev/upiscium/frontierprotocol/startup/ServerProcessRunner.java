package dev.upiscium.frontierprotocol.startup;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

final class ServerProcessRunner {
    private static final Pattern READY = Pattern.compile("\\bDone \\([^)]+\\)! For help, type \\\"help\\\"");
    private static final List<Pattern> TERMINAL_FAILURES = List.of(
            Pattern.compile("(?i)Mod ID:.*Actual version:.*MISSING"),
            Pattern.compile("(?i)Loading errors encountered"),
            Pattern.compile("(?i)ModLoadingException"),
            Pattern.compile("(?i)Failed to load config"),
            Pattern.compile("(?i)Failed loading config file"),
            Pattern.compile("(?i)ParsingException"),
            Pattern.compile("(?i)---- Minecraft Crash Report ----"),
            Pattern.compile("(?i)Mixin apply .* failed|Mixin application failed"));

    enum Expectation {
        SUCCESS,
        TERMINAL_FAILURE,
        SUCCESS_OR_TERMINAL_FAILURE
    }

    record Result(
            boolean ready,
            boolean stopped,
            boolean terminatedAfterFailure,
            int exitCode,
            String terminalFailure,
            List<String> output) {}

    Result run(
            List<String> command,
            Path workingDirectory,
            Path logFile,
            Duration startupTimeout,
            Duration shutdownTimeout,
            Expectation expectation)
            throws Exception {
        Files.createDirectories(workingDirectory);
        Files.createDirectories(logFile.getParent());

        AtomicBoolean ready = new AtomicBoolean();
        AtomicBoolean stopped = new AtomicBoolean();
        AtomicBoolean terminatedAfterFailure = new AtomicBoolean();
        AtomicReference<String> terminalFailure = new AtomicReference<>();
        AtomicReference<Throwable> readerFailure = new AtomicReference<>();
        List<String> output = new ArrayList<>();
        ArrayDeque<String> tail = new ArrayDeque<>(80);
        Process process = null;
        Thread reader = null;
        Thread shutdownHook = null;
        Throwable failure = null;
        try {
            process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            Process runningProcess = process;
            shutdownHook = Thread.ofPlatform().unstarted(() -> terminateWithoutInterrupt(runningProcess));
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            reader = Thread.ofPlatform().daemon().name("stable-startup-matrix-output").start(() -> {
                try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8);
                        var lines = runningProcess.inputReader(StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = lines.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                        writer.flush();
                        synchronized (output) {
                            output.add(line);
                        }
                        synchronized (tail) {
                            if (tail.size() == 80) {
                                tail.removeFirst();
                            }
                            tail.addLast(line);
                        }
                        if (READY.matcher(line).find()) {
                            ready.set(true);
                        }
                        if (line.contains("Stopping server")) {
                            stopped.set(true);
                        }
                        for (Pattern pattern : TERMINAL_FAILURES) {
                            if (pattern.matcher(line).find()) {
                                if (terminalFailure.compareAndSet(null, line)
                                        && expectation == Expectation.TERMINAL_FAILURE) {
                                    terminatedAfterFailure.set(true);
                                    runningProcess.destroy();
                                }
                                break;
                            }
                        }
                    }
                } catch (Throwable thrown) {
                    if (!terminatedAfterFailure.get()) {
                        readerFailure.set(thrown);
                    }
                }
            });

            waitForStartup(process, ready, terminalFailure, readerFailure, startupTimeout, expectation);
            if (ready.get()) {
                if (expectation == Expectation.TERMINAL_FAILURE) {
                    throw new IllegalStateException("Server reached Done when a terminal startup failure was required");
                }
                process.outputWriter(StandardCharsets.UTF_8).append("stop\n").flush();
                if (!process.waitFor(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("Server did not stop within " + shutdownTimeout.toSeconds() + " seconds");
                }
                joinReader(reader, readerFailure);
                if (!stopped.get()) {
                    throw new IllegalStateException("Server did not acknowledge the stop command");
                }
                if (process.exitValue() != 0) {
                    throw new IllegalStateException("Server exited with status " + process.exitValue());
                }
            } else {
                if (expectation == Expectation.SUCCESS) {
                    throw new IllegalStateException("Server reported terminal startup failure: " + terminalFailure.get());
                }
                if (!process.waitFor(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    terminate(process);
                }
                joinReader(reader, readerFailure);
                if (process.exitValue() == 0 && !terminatedAfterFailure.get()) {
                    throw new IllegalStateException("Terminal startup failure exited with status 0");
                }
            }
        } catch (Throwable thrown) {
            failure = thrown;
        } finally {
            if (process != null && process.isAlive()) {
                terminate(process);
            }
            if (reader != null && reader.isAlive()) {
                reader.join(TimeUnit.SECONDS.toMillis(5));
            }
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ignored) {
                    // The registered hook owns cleanup once JVM shutdown has begun.
                }
            }
        }

        if (failure != null) {
            StringBuilder message = new StringBuilder(failure.getMessage() == null ? failure.toString() : failure.getMessage());
            synchronized (tail) {
                if (!tail.isEmpty()) {
                    message.append(System.lineSeparator()).append("Log tail:");
                    tail.forEach(line -> message.append(System.lineSeparator()).append(line));
                }
            }
            throw new IllegalStateException(message.toString(), failure);
        }
        synchronized (output) {
            return new Result(
                    ready.get(),
                    stopped.get(),
                    terminatedAfterFailure.get(),
                    process.exitValue(),
                    terminalFailure.get(),
                    List.copyOf(output));
        }
    }

    static boolean isCanonicalReadyLine(String line) {
        return READY.matcher(line).find();
    }

    private static void waitForStartup(
            Process process,
            AtomicBoolean ready,
            AtomicReference<String> terminalFailure,
            AtomicReference<Throwable> readerFailure,
            Duration timeout,
            Expectation expectation)
            throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!ready.get()) {
            if (readerFailure.get() != null) {
                throw new IllegalStateException("Server log reader failed", readerFailure.get());
            }
            if (terminalFailure.get() != null && expectation == Expectation.TERMINAL_FAILURE) {
                break;
            }
            if (!process.isAlive()) {
                Thread.sleep(100);
                if (terminalFailure.get() == null) {
                    throw new IllegalStateException("Server exited before a conclusive startup result with status "
                            + process.exitValue());
                }
                break;
            }
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("Server startup timed out after " + timeout.toSeconds()
                        + " seconds; timeout is never an expected failure for "
                        + expectation.name().toLowerCase(Locale.ROOT));
            }
            Thread.sleep(100);
        }
    }

    private static void joinReader(Thread reader, AtomicReference<Throwable> readerFailure) throws Exception {
        reader.join(TimeUnit.SECONDS.toMillis(5));
        if (reader.isAlive()) {
            throw new IllegalStateException("Server log reader did not terminate");
        }
        if (readerFailure.get() != null) {
            throw new IllegalStateException("Server log reader failed", readerFailure.get());
        }
    }

    static void terminate(Process process) throws InterruptedException {
        List<ProcessHandle> descendants = process.toHandle().descendants().toList().reversed();
        descendants.forEach(ProcessHandle::destroy);
        process.destroy();
        process.waitFor(5, TimeUnit.SECONDS);
        descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
        if (process.isAlive()) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    private static void terminateWithoutInterrupt(Process process) {
        boolean interrupted = false;
        try {
            terminate(process);
        } catch (InterruptedException ignored) {
            interrupted = true;
            process.toHandle().descendants().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
