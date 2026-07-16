package com.suilearn.api.runtimefixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RuntimeFixtureFaultScriptContractTest {
    @Test
    void assertsTheLowCardinalityOcrTimeoutMetricAfterTheOcrFaultProbe() throws Exception {
        String script = Files.readString(Path.of("..", "..", "scripts", "verify-runtime-faults.ps1"));

        assertThat(script).contains("function Assert-OcrTimeoutMetric");
        assertThat(script).contains("suilearn_ocr_pages_total");
        assertThat(script).contains("outcome=\"timed_out\"");
        assertThat(script).contains("Invoke-FixtureProbe 'ocr'\n    Assert-OcrTimeoutMetric");
    }

    @Test
    void invokesAiTimeoutTwiceThenObservesTheCircuitOpenMetric() throws Exception {
        String script = Files.readString(Path.of("..", "..", "scripts", "verify-runtime-faults.ps1"));

        assertThat(script).contains("function Assert-AiCircuitOpenMetric");
        assertThat(script).contains("outcome=\"circuit_open\"");
        assertThat(script).contains(
            "Invoke-FixtureProbe 'ai'\n    Invoke-FixtureProbe 'ai'\n    Invoke-FixtureProbe 'ai'\n    Assert-AiTimeoutMetric\n    Assert-AiCircuitOpenMetric"
        );
    }
}
