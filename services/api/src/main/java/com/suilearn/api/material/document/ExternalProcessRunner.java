package com.suilearn.api.material.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Starts a trusted executable with an argument vector, never through a shell. */
public interface ExternalProcessRunner {
    RunningExternalProcess start(List<String> command) throws IOException;

    static ExternalProcessRunner processBuilder() {
        return command -> {
            Process process = new ProcessBuilder(List.copyOf(command)).start();
            return new ProcessBackedExternalProcess(process);
        };
    }

    final class ProcessBackedExternalProcess implements RunningExternalProcess {
        private static final int MAX_OUTPUT_BYTES = 1_048_576;
        private final Process process;
        private final CompletableFuture<String> stdout;
        private final CompletableFuture<String> stderr;

        private ProcessBackedExternalProcess(Process process) {
            this.process = process;
            this.stdout = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
            this.stderr = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
        }

        @Override
        public boolean await(java.time.Duration timeout) throws InterruptedException {
            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (finished) {
                stdout.join();
                stderr.join();
            }
            return finished;
        }

        @Override
        public int exitCode() {
            return process.exitValue();
        }

        @Override
        public String stdout() {
            return stdout.join();
        }

        @Override
        public String stderr() {
            return stderr.join();
        }

        @Override
        public void terminate() {
            try (var descendants = process.toHandle().descendants()) {
                descendants.forEach(ProcessHandle::destroyForcibly);
            } finally {
                process.destroyForcibly();
            }
        }

        private static String read(InputStream stream) {
            try (stream) {
                var output = new ByteArrayOutputStream();
                var buffer = new byte[8_192];
                for (int read; (read = stream.read(buffer)) != -1;) {
                    if (output.size() > MAX_OUTPUT_BYTES - read) {
                        throw new IllegalStateException("External process output exceeds " + MAX_OUTPUT_BYTES + " bytes");
                    }
                    output.write(buffer, 0, read);
                }
                return output.toString(StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("External process output could not be read", exception);
            }
        }
    }
}
