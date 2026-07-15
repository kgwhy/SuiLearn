package com.suilearn.api.material.document;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/** OCR adapter that invokes Tesseract with a fixed argument vector. */
public class TesseractOcrAdapter {
    private final String executable;
    private final ExternalProcessRunner runner;
    private final Duration timeout;
    private final String adapterVersion;
    private final Semaphore permits;

    public TesseractOcrAdapter(String executable, ExternalProcessRunner runner, int concurrency, Duration timeout,
                               String adapterVersion) {
        this.executable = Objects.requireNonNull(executable);
        this.runner = Objects.requireNonNull(runner);
        this.timeout = Objects.requireNonNull(timeout);
        this.adapterVersion = Objects.requireNonNull(adapterVersion);
        this.permits = new Semaphore(Math.max(1, concurrency));
    }

    public Result recognize(Path input, String revisionId, int pageNumber) {
        Objects.requireNonNull(input);
        String operationKey = "ocr:" + revisionId + ":page-" + pageNumber + ":" + adapterVersion;
        boolean acquired = false;
        try {
            permits.acquire();
            acquired = true;
            RunningExternalProcess process = runner.start(List.of(executable, "--", input.toString(), "stdout"));
            if (!process.await(timeout)) {
                process.terminate();
                return new Result("TIMED_OUT", "", operationKey);
            }
            return process.exitCode() == 0
                ? new Result("SUCCEEDED", process.stdout(), operationKey)
                : new Result("FAILED", "", operationKey);
        } catch (IOException exception) {
            return new Result("FAILED", "", operationKey);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Result("FAILED", "", operationKey);
        } catch (RuntimeException exception) {
            return new Result("FAILED", "", operationKey);
        } finally {
            if (acquired) {
                permits.release();
            }
        }
    }

    public String adapterVersion() { return adapterVersion; }

    public record Result(String status, String text, String operationKey) {
    }
}
