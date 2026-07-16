package com.suilearn.api.runtimefixture;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.ai.AiFailureKind;
import com.suilearn.api.ai.AiOperationalMetrics;
import com.suilearn.api.ai.OpenAiCompatibleAiProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

/** Profile-scoped AI proxy that creates deterministic timeout and circuit-open outcomes without external traffic. */
@Component
@Primary
@Profile("runtime-fixture")
public final class RuntimeFixtureAiProvider implements AiProvider {
    private final RuntimeFixtureControl control;
    private final AiProvider delegate;
    private final AiOperationalMetrics metrics;
    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("runtime-fixture-ai", CircuitBreakerConfig.custom()
        .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(50).build());

    @Autowired
    public RuntimeFixtureAiProvider(RuntimeFixtureControl control, OpenAiCompatibleAiProvider delegate, MeterRegistry meterRegistry) {
        this(control, (AiProvider) delegate, new AiOperationalMetrics(meterRegistry));
    }

    RuntimeFixtureAiProvider(RuntimeFixtureControl control, AiProvider delegate) {
        this(control, delegate, AiOperationalMetrics.noop());
    }

    RuntimeFixtureAiProvider(RuntimeFixtureControl control, AiProvider delegate, AiOperationalMetrics metrics) {
        this.control = control;
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public GeneratedQuestion generateQuestion(QuestionGenerationPrompt prompt) {
        return invoke(() -> delegate.generateQuestion(prompt));
    }

    @Override
    public List<GeneratedKnowledgePoint> extractKnowledgePoints(KnowledgePointExtractionPrompt prompt) {
        return invoke(() -> delegate.extractKnowledgePoints(prompt));
    }

    @Override
    public GeneratedNote generateKnowledgePointExplanation(KnowledgePointExplanationPrompt prompt) {
        return invoke(() -> delegate.generateKnowledgePointExplanation(prompt));
    }

    @Override
    public GeneratedNote generateReviewSuggestion(ReviewSuggestionPrompt prompt) {
        return invoke(() -> delegate.generateReviewSuggestion(prompt));
    }

    @Override
    public GeneratedAnswer answerQuestion(AnswerQuestionPrompt prompt) {
        return invoke(() -> delegate.answerQuestion(prompt));
    }

    void resetCircuitBreaker() {
        circuitBreaker.reset();
    }

    private <T> T invoke(Supplier<T> action) {
        long startedAt = System.nanoTime();
        try {
            T result = circuitBreaker.executeSupplier(() -> {
                if (control.aiMode() == RuntimeFixtureControl.Mode.TIMEOUT) {
                    throw new RuntimeException("Runtime fixture forced AI timeout", new HttpTimeoutException("fixture timeout"));
                }
                return action.get();
            });
            metrics.record("chat", AiFailureKind.SUCCESS, System.nanoTime() - startedAt);
            return result;
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException exception) {
            metrics.record("chat", AiFailureKind.CIRCUIT_OPEN, System.nanoTime() - startedAt);
            throw exception;
        } catch (RuntimeException exception) {
            metrics.record("chat", AiFailureKind.TIMEOUT, System.nanoTime() - startedAt);
            throw exception;
        }
    }
}
