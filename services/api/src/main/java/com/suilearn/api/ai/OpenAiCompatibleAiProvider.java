package com.suilearn.api.ai;

import com.suilearn.api.config.SuiLearnAiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "suilearn.ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleAiProvider implements AiProvider {
    private final SuiLearnAiProperties properties;

    public OpenAiCompatibleAiProvider(SuiLearnAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public GeneratedQuestion generateQuestion(QuestionGenerationPrompt prompt) {
        throw unavailable();
    }

    @Override
    public GeneratedNote generateKnowledgePointExplanation(KnowledgePointExplanationPrompt prompt) {
        throw unavailable();
    }

    @Override
    public GeneratedNote generateReviewSuggestion(ReviewSuggestionPrompt prompt) {
        throw unavailable();
    }

    private IllegalStateException unavailable() {
        var message = properties.hasOpenAiCompatibleConfiguration()
            ? "OpenAI-compatible HTTP generation is not enabled in this MVP"
            : "OpenAI-compatible provider is not fully configured";
        return new IllegalStateException(message);
    }
}
