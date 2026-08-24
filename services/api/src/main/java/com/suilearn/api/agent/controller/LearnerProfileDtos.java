package com.suilearn.api.agent.controller;

import com.suilearn.api.agent.learner.LearnerProfile;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class LearnerProfileDtos {
    private LearnerProfileDtos() {}

    public record LearnerProfileRequest(
        @Size(max = 2000) String persona,
        @Size(max = 20) List<@Size(min = 1, max = 100) String> skills
    ) {}

    public record LearnerProfileResponse(String learnerId, String persona, List<String> skills) {
        public LearnerProfileResponse {
            skills = List.copyOf(skills == null ? List.of() : skills);
        }

        public static LearnerProfileResponse from(LearnerProfile profile) {
            return new LearnerProfileResponse(profile.learnerId(), profile.persona(), profile.skills());
        }
    }
}
