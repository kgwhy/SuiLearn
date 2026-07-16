package com.suilearn.api.runtimefixture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.suilearn.api.ai.AiProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeFixtureAiProviderTest {
    @Test
    void deterministicallyTimesOutAndThenOpensItsCircuitWhenTheAiFaultModeIsEnabled() {
        var control = new RuntimeFixtureControl();
        control.setAiMode(RuntimeFixtureControl.Mode.TIMEOUT);
        var provider = new RuntimeFixtureAiProvider(control, mock(AiProvider.class));
        var prompt = new AiProvider.KnowledgePointExtractionPrompt("kb", "material", "title", List.of(), 1);

        assertThatThrownBy(() -> provider.extractKnowledgePoints(prompt)).hasCauseInstanceOf(java.net.http.HttpTimeoutException.class);
        assertThatThrownBy(() -> provider.extractKnowledgePoints(prompt)).hasCauseInstanceOf(java.net.http.HttpTimeoutException.class);
        assertThatThrownBy(() -> provider.extractKnowledgePoints(prompt))
            .isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);

        provider.resetCircuitBreaker();

        assertThatThrownBy(() -> provider.extractKnowledgePoints(prompt)).hasCauseInstanceOf(java.net.http.HttpTimeoutException.class);
    }
}
