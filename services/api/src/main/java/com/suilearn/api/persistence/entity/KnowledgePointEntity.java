package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_points")
public class KnowledgePointEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String name;
    private String description;
    private String sourceMaterialId;
    @Column(columnDefinition = "text")
    private String sourceRefsJson;

    protected KnowledgePointEntity() {
    }

    public KnowledgePointEntity(
        String id,
        String knowledgeBaseId,
        String name,
        String description,
        String sourceMaterialId,
        String sourceRefsJson
    ) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.name = name;
        this.description = description;
        this.sourceMaterialId = sourceMaterialId;
        this.sourceRefsJson = sourceRefsJson;
    }

    public String getId() {
        return id;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceMaterialId() {
        return sourceMaterialId;
    }

    public String getSourceRefsJson() {
        return sourceRefsJson;
    }
}
