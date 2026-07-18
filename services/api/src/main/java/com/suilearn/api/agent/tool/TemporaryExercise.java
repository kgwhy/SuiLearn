package com.suilearn.api.agent.tool;

import java.util.List;

public record TemporaryExercise(String question, String answer, String explanation, List<String> citations) {
    public TemporaryExercise {
        question = RequiredText.value(question, "question");
        answer = RequiredText.value(answer, "answer");
        explanation = RequiredText.value(explanation, "explanation");
        citations = List.copyOf(citations == null ? List.of() : citations);
    }
}
