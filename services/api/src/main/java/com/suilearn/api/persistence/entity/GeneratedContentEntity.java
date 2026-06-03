package com.suilearn.api.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "generated_contents")
public class GeneratedContentEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String generationTaskId;
    private String status;
    @Lob
    private String sourceRefsJson;
    private String sourceType;
    private String sourceId;
    private String questionType;
    private String categoryId;
    private String categoryName;
    @Lob
    private String knowledgePointIdsJson;
    @Lob
    private String stem;
    @Lob
    private String optionsJson;
    @Lob
    private String answerJson;
    @Lob
    private String explanation;
    private String savedQuestionId;
    private Instant savedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected GeneratedContentEntity() {
    }

    public GeneratedContentEntity(
        String id,
        String knowledgeBaseId,
        String generationTaskId,
        String status,
        String sourceRefsJson,
        String sourceType,
        String sourceId,
        String questionType,
        String categoryId,
        String categoryName,
        String knowledgePointIdsJson,
        String stem,
        String optionsJson,
        String answerJson,
        String explanation,
        String savedQuestionId,
        Instant savedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.generationTaskId = generationTaskId;
        this.status = status;
        this.sourceRefsJson = sourceRefsJson;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.questionType = questionType;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.knowledgePointIdsJson = knowledgePointIdsJson;
        this.stem = stem;
        this.optionsJson = optionsJson;
        this.answerJson = answerJson;
        this.explanation = explanation;
        this.savedQuestionId = savedQuestionId;
        this.savedAt = savedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public String getGenerationTaskId() { return generationTaskId; }
    public String getStatus() { return status; }
    public String getSourceRefsJson() { return sourceRefsJson; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public String getQuestionType() { return questionType; }
    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getKnowledgePointIdsJson() { return knowledgePointIdsJson; }
    public String getStem() { return stem; }
    public String getOptionsJson() { return optionsJson; }
    public String getAnswerJson() { return answerJson; }
    public String getExplanation() { return explanation; }
    public String getSavedQuestionId() { return savedQuestionId; }
    public Instant getSavedAt() { return savedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
