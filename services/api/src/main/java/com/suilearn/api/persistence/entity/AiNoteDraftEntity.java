package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_note_drafts")
public class AiNoteDraftEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String generationTaskId;
    private String type;
    private String title;
    @Column(columnDefinition = "text")
    private String content;
    @Column(columnDefinition = "text")
    private String sourceRefsJson;
    private Instant createdAt;

    protected AiNoteDraftEntity() {
    }

    public AiNoteDraftEntity(String id, String knowledgeBaseId, String generationTaskId, String type, String title, String content, String sourceRefsJson, Instant createdAt) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.generationTaskId = generationTaskId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.sourceRefsJson = sourceRefsJson;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public String getGenerationTaskId() { return generationTaskId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSourceRefsJson() { return sourceRefsJson; }
    public Instant getCreatedAt() { return createdAt; }
}
