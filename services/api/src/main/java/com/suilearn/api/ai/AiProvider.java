package com.suilearn.api.ai;

import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import java.util.List;

public interface AiProvider {
    GeneratedQuestion generateQuestion(QuestionGenerationPrompt prompt);

    GeneratedNote generateKnowledgePointExplanation(KnowledgePointExplanationPrompt prompt);

    GeneratedNote generateReviewSuggestion(ReviewSuggestionPrompt prompt);

    record QuestionGenerationPrompt(
        String knowledgeBaseId,
        List<SourceRef> sourceRefs,
        SourceType sourceType,
        String sourceId,
        QuestionType questionType,
        String categoryId,
        String categoryName,
        List<String> knowledgePointIds,
        String userPrompt
    ) {
    }

    record KnowledgePointExplanationPrompt(
        String knowledgeBaseId,
        String knowledgePointId,
        String knowledgePointName,
        String knowledgePointDescription,
        List<SourceRef> sourceRefs,
        String userPrompt
    ) {
    }

    record ReviewSuggestionPrompt(
        String knowledgeBaseId,
        List<SourceRef> sourceRefs,
        List<String> weakKnowledgePointIds,
        List<String> wrongQuestionIds,
        String userPrompt
    ) {
    }

    record GeneratedQuestion(
        QuestionType questionType,
        String categoryId,
        String categoryName,
        List<String> knowledgePointIds,
        String stem,
        List<String> options,
        List<String> answer,
        String explanation
    ) {
    }

    record GeneratedNote(
        String title,
        String content
    ) {
    }
}
