package com.suilearn.api.agent.tool;

import java.util.List;

public record AskUserPayload(String questionId, String prompt, List<AskOption> options, boolean multiSelect) {
    public AskUserPayload {
        questionId = requireText(questionId, "questionId");
        prompt = requireText(prompt, "prompt");
        options = List.copyOf(options == null ? List.of() : options);
    }

    public record AskOption(String id, String label) {
        public AskOption {
            id = requireText(id, "id");
            label = requireText(label, "label");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
