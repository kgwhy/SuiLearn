package com.suilearn.api.generation.domain;

import com.suilearn.api.model.QuestionType;
import java.util.List;
import java.util.Set;

public class QuestionTypeContract {
    private static final Set<QuestionType> SUPPORTED_TYPES = Set.of(
        QuestionType.SINGLE_CHOICE,
        QuestionType.MULTIPLE_CHOICE,
        QuestionType.TRUE_FALSE,
        QuestionType.SHORT_ANSWER
    );

    public void validate(QuestionType questionType, List<String> options, List<String> answer) {
        if (!SUPPORTED_TYPES.contains(questionType)) {
            throw new IllegalArgumentException("Unsupported question type: " + questionType);
        }
        if ((questionType == QuestionType.SINGLE_CHOICE || questionType == QuestionType.MULTIPLE_CHOICE)
            && (options == null || options.isEmpty())) {
            throw new IllegalArgumentException("Choice questions require options");
        }
        if (answer == null || answer.isEmpty()) {
            throw new IllegalArgumentException("Generated question requires answer");
        }
    }
}
