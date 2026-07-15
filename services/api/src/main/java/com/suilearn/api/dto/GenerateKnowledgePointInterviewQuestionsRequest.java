package com.suilearn.api.dto;

import com.suilearn.api.generation.domain.InterviewQuestionDifficulty;
import com.suilearn.api.model.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateKnowledgePointInterviewQuestionsRequest(
    @Min(1) @Max(10) Integer quantity,
    InterviewQuestionDifficulty difficulty,
    QuestionType questionType
) {
    /** Compatibility constructor; route ownership now comes exclusively from the knowledge-point path. */
    public GenerateKnowledgePointInterviewQuestionsRequest(
        String ignoredKnowledgePointId, Integer quantity, InterviewQuestionDifficulty difficulty, QuestionType questionType
    ) {
        this(quantity, difficulty, questionType);
    }
}
