package com.suilearn.api.material.document;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Preview adapter that invokes LibreOffice headlessly without shell interpolation. */
public class LibreOfficePreviewAdapter {
    private final String executable;
    private final ExternalProcessRunner runner;
    private final Duration timeout;
    private final String adapterVersion;

    public LibreOfficePreviewAdapter(String executable, ExternalProcessRunner runner, Duration timeout,
                                     String adapterVersion) {
        this.executable = Objects.requireNonNull(executable);
        this.runner = Objects.requireNonNull(runner);
        this.timeout = Objects.requireNonNull(timeout);
        this.adapterVersion = Objects.requireNonNull(adapterVersion);
    }

    public Result preview(Path original, String revisionId) {
        Objects.requireNonNull(original);
        String originalReference = original.toString();
        String operationKey = "preview:" + revisionId + ":" + adapterVersion;
        Path outputDirectory = null;
        Path profileDirectory = null;
        try {
            outputDirectory = Files.createTempDirectory("suilearn-preview-output-");
            profileDirectory = Files.createTempDirectory("suilearn-preview-profile-");
            RunningExternalProcess process = runner.start(List.of(
                executable, "--headless", "--safe-mode", "--norestore", "--nodefault", "--nolockcheck",
                "-env:UserInstallation=" + profileDirectory.toUri(), "--outdir=" + outputDirectory,
                "--convert-to", "pdf", "--", originalReference));
            if (!process.await(timeout)) {
                process.terminate();
                return new Result("TIMED_OUT", originalReference, null, operationKey);
            }
            if (process.exitCode() != 0) {
                return new Result("FAILED", originalReference, null, operationKey);
            }
            var preview = findPreview(outputDirectory);
            return preview == null
                ? new Result("FAILED", originalReference, null, operationKey)
                : new Result("SUCCEEDED", originalReference, preview.toString(), operationKey);
        } catch (IOException exception) {
            return new Result("FAILED", originalReference, null, operationKey);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Result("FAILED", originalReference, null, operationKey);
        } catch (RuntimeException exception) {
            return new Result("FAILED", originalReference, null, operationKey);
        } finally {
            deleteTree(profileDirectory);
        }
    }

    private Path findPreview(Path outputDirectory) throws IOException {
        try (var files = Files.list(outputDirectory)) {
            return files.filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf"))
                .findFirst().orElse(null);
        }
    }

    private void deleteTree(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    public record Result(String status, String originalReference, String previewReference, String operationKey) {
    }
}
