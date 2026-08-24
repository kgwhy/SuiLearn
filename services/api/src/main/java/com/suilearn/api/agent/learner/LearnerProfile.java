package com.suilearn.api.agent.learner;

import java.util.List;

public record LearnerProfile(String learnerId, String persona, List<String> skills) {
    public LearnerProfile {
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("learnerId is required");
        }
        learnerId = learnerId.strip();
        persona = persona == null ? "" : persona.strip();
        skills = List.copyOf(skills == null ? List.of() : skills);
    }
}
