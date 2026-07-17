package com.suilearn.api.ai;

import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.generation.domain.InterviewQuestionDifficulty;
import java.util.List;

public interface AiProvider {
    GeneratedQuestion generateQuestion(QuestionGenerationPrompt prompt);

    List<GeneratedKnowledgePoint> extractKnowledgePoints(KnowledgePointExtractionPrompt prompt);

    List<GeneratedKnowledgePoint> repairKnowledgePointExtraction(
        KnowledgePointExtractionPrompt prompt, List<String> validationFailures
    );

    GeneratedNote generateKnowledgePointExplanation(KnowledgePointExplanationPrompt prompt);

    GeneratedNote generateReviewSuggestion(ReviewSuggestionPrompt prompt);

    GeneratedAnswer answerQuestion(AnswerQuestionPrompt prompt);

    record QuestionGenerationPrompt(
        String knowledgeBaseId,
        List<SourceRef> sourceRefs,
        SourceType sourceType,
        String sourceId,
        QuestionType questionType,
        String categoryId,
        String categoryName,
        List<String> knowledgePointIds,
        String userPrompt,
        InterviewQuestionDifficulty difficulty
    ) {
        public QuestionGenerationPrompt(String knowledgeBaseId, List<SourceRef> sourceRefs, SourceType sourceType, String sourceId,
            QuestionType questionType, String categoryId, String categoryName, List<String> knowledgePointIds, String userPrompt) {
            this(knowledgeBaseId, sourceRefs, sourceType, sourceId, questionType, categoryId, categoryName, knowledgePointIds, userPrompt, null);
        }
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

    record KnowledgePointExtractionPrompt(
        String knowledgeBaseId,
        String materialId,
        String materialTitle,
        List<SourceRef> evidenceRefs,
        int maxKnowledgePoints
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

    record AnswerQuestionPrompt(
        String knowledgeBaseId,
        String materialId,
        String question,
        List<SourceRef> sourceRefs,
        List<AnswerEvidence> evidence
    ) {
        public AnswerQuestionPrompt(
            String knowledgeBaseId,
            String materialId,
            String question,
            List<SourceRef> sourceRefs
        ) {
            this(knowledgeBaseId, materialId, question, sourceRefs, List.of());
        }
    }

    record AnswerEvidence(
        int citationNumber,
        SourceRef sourceRef,
        String content,
        double score
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

    record GeneratedAnswer(
        String answer,
        boolean uncertain,
        List<GeneratedStatement> statements
    ) {
        public GeneratedAnswer(String answer, boolean uncertain) {
            this(answer, uncertain, List.of());
        }
    }

    record GeneratedStatement(
        String text,
        List<Integer> citations
    ) {
    }

    record GeneratedKnowledgePoint(
        String name,
        String description,
        String title,
        String shortSummary,
        String definition,
        List<String> principles,
        List<String> applicationScenarios,
        List<String> pitfalls,
        List<SourceRef> citations
    ) {
        public GeneratedKnowledgePoint(String name, String description) {
            this(name, description, null, null, null, null, null, null, null);
        }
    }
}
