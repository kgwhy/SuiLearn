package com.suilearn.api.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_notes")
public class AiNoteEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String type;
    private String title;
    @Lob
    private String content;
    @Lob
    private String sourceRefsJson;
    private Instant savedAt;

    protected AiNoteEntity() {
    }

    public AiNoteEntity(String id, String knowledgeBaseId, String type, String title, String content, String sourceRefsJson, Instant savedAt) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.sourceRefsJson = sourceRefsJson;
        this.savedAt = savedAt;
    }

    public String getId() { return id; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSourceRefsJson() { return sourceRefsJson; }
    public Instant getSavedAt() { return savedAt; }
}
