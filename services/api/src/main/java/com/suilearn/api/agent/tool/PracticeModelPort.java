package com.suilearn.api.agent.tool;

import java.util.List;

@FunctionalInterface
public interface PracticeModelPort {
    Draft generate(Request request);

    record Request(String learningGoal, EvidenceBundle evidence, PracticeDifficulty difficulty, int practiceCount) {
        public Request {
            learningGoal = RequiredText.value(learningGoal, "learningGoal");
            if (evidence == null || difficulty == null) {
                throw new IllegalArgumentException("evidence and difficulty are required");
            }
            if (practiceCount < 1 || practiceCount > 5) {
                throw new IllegalArgumentException("practiceCount must be between 1 and 5");
            }
        }
    }

    record Draft(
        String explanation,
        List<TemporaryExercise> exercises,
        List<String> citations,
        String nextStep
    ) {
        public Draft {
            explanation = RequiredText.value(explanation, "explanation");
            exercises = List.copyOf(exercises == null ? List.of() : exercises);
            citations = List.copyOf(citations == null ? List.of() : citations);
            nextStep = nextStep == null ? "" : nextStep;
        }
    }
}
