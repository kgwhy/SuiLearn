package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "material_chunks")
public class MaterialChunkEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String materialId;
    @Lob
    private String content;
    @Column(name = "chunk_ordinal")
    private int ordinal;
    @Lob
    private String sourceRefJson;
    @Lob
    private String embeddingJson;
    private String embeddingStatus;
    private String embeddingModel;
    private Integer embeddingDimensions;

    protected MaterialChunkEntity() {
    }

    public MaterialChunkEntity(
        String id,
        String knowledgeBaseId,
        String materialId,
        String content,
        int ordinal,
        String sourceRefJson,
        String embeddingJson,
        String embeddingStatus,
        String embeddingModel,
        Integer embeddingDimensions
    ) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.materialId = materialId;
        this.content = content;
        this.ordinal = ordinal;
        this.sourceRefJson = sourceRefJson;
        this.embeddingJson = embeddingJson;
        this.embeddingStatus = embeddingStatus;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
    }

    public String getId() {
        return id;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getContent() {
        return content;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public String getSourceRefJson() {
        return sourceRefJson;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public String getEmbeddingStatus() {
        return embeddingStatus;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public Integer getEmbeddingDimensions() {
        return embeddingDimensions;
    }
}
