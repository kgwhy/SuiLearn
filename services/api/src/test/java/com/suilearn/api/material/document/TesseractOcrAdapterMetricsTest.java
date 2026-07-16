package com.suilearn.api.material.document;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TesseractOcrAdapterMetricsTest {
    @Test
    void recordsATimedOutPageWhenTheExternalProcessExceedsTheBoundedTimeout() {
        var registry = new SimpleMeterRegistry();
        var adapter = new TesseractOcrAdapter("tesseract", command -> new TimedOutProcess(), 1,
            Duration.ofMillis(1), "tesseract-v1", new OcrOperationalMetrics(registry));

        var result = adapter.recognize(Path.of("input.png"), "revision-sensitive", 7);

        assertThat(result.status()).isEqualTo("TIMED_OUT");
        assertThat(registry.find("suilearn.ocr.pages").tag("outcome", "timed_out").counter().count()).isEqualTo(1);
    }

    private static final class TimedOutProcess implements RunningExternalProcess {
        @Override public boolean await(Duration timeout) { return false; }
        @Override public int exitCode() { return 1; }
        @Override public String stdout() { return ""; }
        @Override public String stderr() { return ""; }
        @Override public void terminate() { }
    }
}
