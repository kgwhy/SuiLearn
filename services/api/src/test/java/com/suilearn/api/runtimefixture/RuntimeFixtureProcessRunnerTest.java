package com.suilearn.api.runtimefixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.suilearn.api.material.document.ExternalProcessRunner;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeFixtureProcessRunnerTest {
    @Test
    void returnsATimedOutProcessWithoutStartingAnExternalCommandWhenOcrTimeoutIsEnabled() throws Exception {
        var control = new RuntimeFixtureControl();
        control.setOcrMode(RuntimeFixtureControl.Mode.TIMEOUT);
        var fallback = mock(ExternalProcessRunner.class);
        var runner = new RuntimeFixtureProcessRunner(control, fallback);

        var process = runner.start(List.of("tesseract", "--", "ignored", "stdout"));

        assertThat(process.await(Duration.ofMillis(1))).isFalse();
        verifyNoInteractions(fallback);
    }
}
