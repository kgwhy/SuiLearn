package com.suilearn.api.agent.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record TurnReply(String text, Map<String, Object> answers) {
    public TurnReply {
        text = text == null ? "" : text;
        answers = immutableCopy(answers);
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
