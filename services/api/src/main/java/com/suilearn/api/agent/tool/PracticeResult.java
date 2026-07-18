package com.suilearn.api.agent.tool;

import java.util.List;

public record PracticeResult(
    String explanation,
    List<TemporaryExercise> exercises,
    List<String> citations,
    String nextStep,
    boolean uncertain
) {
    public PracticeResult {
        explanation = explanation == null ? "" : explanation;
        exercises = List.copyOf(exercises == null ? List.of() : exercises);
        citations = List.copyOf(citations == null ? List.of() : citations);
        nextStep = nextStep == null ? "" : nextStep;
        if (uncertain && (!exercises.isEmpty() || !citations.isEmpty())) {
            throw new IllegalArgumentException("uncertain result cannot contain exercises or citations");
        }
    }

    public static PracticeResult noEvidence() {
        return new PracticeResult("", List.of(), List.of(), "", true);
    }
}
