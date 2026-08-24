package com.suilearn.api.security;

public record LearnerPrincipal(String learnerId) {
    public LearnerPrincipal {
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("learnerId is required");
        }
        learnerId = learnerId.strip();
    }

    public static LearnerPrincipal fromAuthentication(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof LearnerPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
