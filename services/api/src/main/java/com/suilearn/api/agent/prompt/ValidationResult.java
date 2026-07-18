package com.suilearn.api.agent.prompt;

import java.util.List;

public record ValidationResult(boolean valid, List<String> reasonCodes) {
    public ValidationResult {
        reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes);
        if (valid && !reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("valid result cannot contain reason codes");
        }
        if (!valid && reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("invalid result requires a reason code");
        }
    }

    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(String... reasonCodes) {
        return new ValidationResult(false, List.of(reasonCodes));
    }
}
