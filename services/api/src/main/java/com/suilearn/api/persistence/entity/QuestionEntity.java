package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class QuestionEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String questionType;
    @Column(columnDefinition = "text")
    private String stem;
    private String categoryId;
    private String categoryName;
    private Integer difficulty;
    @Column(columnDefinition = "text")
    private String knowledgePointIdsJson;
    private int answeredCount;
    private double correctRate;
    @Column(columnDefinition = "text")
    private String sourceRefsJson;
    private Instant createdAt;
    private Instant savedAt;

    protected QuestionEntity() {
    }

    public QuestionEntity(
        String id,
        String knowledgeBaseId,
        String questionType,
        String stem,
        String categoryId,
        String categoryName,
        Integer difficulty,
        String knowledgePointIdsJson,
        int answeredCount,
        double correctRate,
        String sourceRefsJson,
        Instant createdAt,
        Instant savedAt
    ) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.questionType = questionType;
        this.stem = stem;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.knowledgePointIdsJson = knowledgePointIdsJson;
        this.answeredCount = answeredCount;
        this.correctRate = correctRate;
        this.sourceRefsJson = sourceRefsJson;
        this.createdAt = createdAt;
        this.savedAt = savedAt;
    }

    public String getId() { return id; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public String getQuestionType() { return questionType; }
    public String getStem() { return stem; }
    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Integer getDifficulty() { return difficulty; }
    public String getKnowledgePointIdsJson() { return knowledgePointIdsJson; }
    public int getAnsweredCount() { return answeredCount; }
    public double getCorrectRate() { return correctRate; }
    public String getSourceRefsJson() { return sourceRefsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSavedAt() { return savedAt; }
}
