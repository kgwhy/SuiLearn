package com.suilearn.api.agent.learner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerProfileService {
    private static final int MAX_PERSONA_LENGTH = 2000;
    private static final int MAX_SKILLS = 20;
    private static final int MAX_SKILL_LENGTH = 100;
    private static final TypeReference<List<String>> SKILLS = new TypeReference<>() { };

    private final LearnerProfileJpaRepository profiles;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LearnerProfileService(LearnerProfileJpaRepository profiles, ObjectMapper objectMapper, Clock clock) {
        this.profiles = profiles;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<LearnerProfile> get(String learnerId) {
        requireLearnerId(learnerId);
        return profiles.findById(learnerId).map(this::toProfile);
    }

    @Transactional
    public LearnerProfile save(String learnerId, String persona, List<String> skills) {
        requireLearnerId(learnerId);
        String normalizedPersona = persona == null ? "" : persona.strip();
        if (normalizedPersona.length() > MAX_PERSONA_LENGTH) {
            throw new IllegalArgumentException("persona must be at most " + MAX_PERSONA_LENGTH + " characters");
        }
        List<String> normalizedSkills = skills == null ? List.of() : skills.stream()
            .map(this::normalizeSkill).toList();
        if (normalizedSkills.size() > MAX_SKILLS) {
            throw new IllegalArgumentException("skills must contain at most " + MAX_SKILLS + " items");
        }
        String skillsJson = writeSkills(normalizedSkills);
        profiles.save(new LearnerProfileEntity(learnerId, normalizedPersona, skillsJson, clock.instant()));
        return new LearnerProfile(learnerId, normalizedPersona, normalizedSkills);
    }

    private LearnerProfile toProfile(LearnerProfileEntity entity) {
        return new LearnerProfile(entity.getLearnerId(), entity.getPersona(), readSkills(entity.getSkillsJson()));
    }

    private String normalizeSkill(String skill) {
        String normalized = skill == null ? "" : skill.strip();
        if (normalized.isEmpty() || normalized.length() > MAX_SKILL_LENGTH) {
            throw new IllegalArgumentException("each skill must be 1.." + MAX_SKILL_LENGTH + " characters");
        }
        return normalized;
    }

    private String writeSkills(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize learner skills", exception);
        }
    }

    private List<String> readSkills(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, SKILLS);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to deserialize learner skills", exception);
        }
    }

    private static void requireLearnerId(String learnerId) {
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("learnerId is required");
        }
    }
}
