package com.suilearn.api.agent.learner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "learner_profile")
public class LearnerProfileEntity {
    @Id private String learnerId;
    @Column(columnDefinition = "text") private String persona;
    @Column(columnDefinition = "text") private String skillsJson;
    private Instant updatedAt;

    protected LearnerProfileEntity() {}

    public LearnerProfileEntity(String learnerId, String persona, String skillsJson, Instant updatedAt) {
        this.learnerId = learnerId;
        this.persona = persona;
        this.skillsJson = skillsJson;
        this.updatedAt = updatedAt;
    }

    public String getLearnerId() { return learnerId; }
    public String getPersona() { return persona; }
    public String getSkillsJson() { return skillsJson; }
    public Instant getUpdatedAt() { return updatedAt; }
}
