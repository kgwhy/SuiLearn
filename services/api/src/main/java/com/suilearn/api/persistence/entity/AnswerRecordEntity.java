package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "answer_records")
public class AnswerRecordEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String questionId;
    @Column(columnDefinition = "text")
    private String userAnswerJson;
    private boolean correct;
    private long durationMs;
    private Instant answeredAt;

    protected AnswerRecordEntity() {
    }

    public AnswerRecordEntity(
        String id,
        String knowledgeBaseId,
        String questionId,
        String userAnswerJson,
        boolean correct,
        long durationMs,
        Instant answeredAt
    ) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.questionId = questionId;
        this.userAnswerJson = userAnswerJson;
        this.correct = correct;
        this.durationMs = durationMs;
        this.answeredAt = answeredAt;
    }

    public String getId() { return id; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public String getQuestionId() { return questionId; }
    public String getUserAnswerJson() { return userAnswerJson; }
    public boolean isCorrect() { return correct; }
    public long getDurationMs() { return durationMs; }
    public Instant getAnsweredAt() { return answeredAt; }
}
