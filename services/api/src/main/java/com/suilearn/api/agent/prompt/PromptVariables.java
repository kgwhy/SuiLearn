package com.suilearn.api.agent.prompt;

import java.util.LinkedHashMap;
import java.util.Map;

public sealed interface PromptVariables permits PromptVariables.Supervisor, PromptVariables.KnowledgeResearch,
    PromptVariables.PracticeCoach, PromptVariables.MemoryExtraction {

    Map<String, String> values();

    record Supervisor(String task, String scope, String context) implements PromptVariables {
        @Override public Map<String, String> values() {
            return ordered("task", task, "scope", scope, "context", context);
        }
    }

    record KnowledgeResearch(String researchGoal, String scope, String learningMemory) implements PromptVariables {
        @Override public Map<String, String> values() {
            return ordered("researchGoal", researchGoal, "scope", scope, "learningMemory", learningMemory);
        }
    }

    record PracticeCoach(String learningGoal, String evidenceBundle, String difficulty,
                         String practiceCount, String outputSchema) implements PromptVariables {
        @Override public Map<String, String> values() {
            return ordered("learningGoal", learningGoal, "evidenceBundle", evidenceBundle,
                "difficulty", difficulty, "practiceCount", practiceCount, "outputSchema", outputSchema);
        }
    }

    record MemoryExtraction(String verifiedOutcome, String sourceReference, String outputSchema)
        implements PromptVariables {
        @Override public Map<String, String> values() {
            return ordered("verifiedOutcome", verifiedOutcome, "sourceReference", sourceReference,
                "outputSchema", outputSchema);
        }
    }

    private static Map<String, String> ordered(String... pairs) {
        var values = new LinkedHashMap<String, String>();
        for (int index = 0; index < pairs.length; index += 2) {
            String value = pairs[index + 1];
            if (value == null) {
                throw new IllegalArgumentException(pairs[index] + " is required");
            }
            values.put(pairs[index], value);
        }
        return Map.copyOf(values);
    }
}
