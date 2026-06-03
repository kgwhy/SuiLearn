package com.suilearn.api.ai;

import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "suilearn.ai", name = "provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiProvider implements AiProvider {
    @Override
    public GeneratedQuestion generateQuestion(QuestionGenerationPrompt prompt) {
        var topic = firstSourceTitle(prompt.sourceRefs());
        var questionType = prompt.questionType() == null ? QuestionType.SINGLE_CHOICE : prompt.questionType();
        return new GeneratedQuestion(
            questionType,
            prompt.categoryId(),
            prompt.categoryName(),
            prompt.knowledgePointIds() == null ? List.of() : prompt.knowledgePointIds(),
            "Fake AI question about " + topic + ": which statement is most accurate?",
            optionsFor(questionType),
            answerFor(questionType),
            "Fake AI explanation: review the cited source for " + topic
                + " before saving this generated question."
        );
    }

    @Override
    public GeneratedNote generateKnowledgePointExplanation(KnowledgePointExplanationPrompt prompt) {
        return new GeneratedNote(
            prompt.knowledgePointName() + " explanation",
            "Fake AI explanation for " + prompt.knowledgePointName()
                + ": focus on the definition, the common pitfall, and one source-backed example."
        );
    }

    @Override
    public GeneratedNote generateReviewSuggestion(ReviewSuggestionPrompt prompt) {
        var weakPointCount = prompt.weakKnowledgePointIds() == null ? 0 : prompt.weakKnowledgePointIds().size();
        return new GeneratedNote(
            weakPointCount == 0 ? "Review suggestion" : "Weak knowledge point review suggestion",
            "Fake AI review suggestion: redo related questions, revisit the weakest knowledge points,"
                + " and generate one focused practice set before marking the topic as mastered."
        );
    }

    private List<String> optionsFor(QuestionType questionType) {
        return switch (questionType) {
            case MULTIPLE_CHOICE -> List.of(
                "A. It should be checked against the cited source.",
                "B. It can be saved only after user review.",
                "C. It ignores source traceability.",
                "D. It should replace all existing questions automatically."
            );
            case TRUE_FALSE -> List.of("True", "False");
            case SHORT_ANSWER -> List.of();
            case SINGLE_CHOICE -> List.of(
                "A. It should be checked against the cited source.",
                "B. It ignores source traceability.",
                "C. It should replace all existing questions automatically.",
                "D. It does not need user review."
            );
        };
    }

    private List<String> answerFor(QuestionType questionType) {
        return switch (questionType) {
            case MULTIPLE_CHOICE -> List.of("A", "B");
            case TRUE_FALSE -> List.of("True");
            case SHORT_ANSWER -> List.of("Check the generated content against source evidence before saving.");
            case SINGLE_CHOICE -> List.of("A");
        };
    }

    private String firstSourceTitle(List<SourceRef> sourceRefs) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            return "current source";
        }
        var first = sourceRefs.get(0);
        if (first.title() != null && !first.title().isBlank()) {
            return first.title();
        }
        return first.id();
    }
}
