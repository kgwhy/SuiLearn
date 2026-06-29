package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "material_chunks")
public class MaterialChunkEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String materialId;
    @Column(columnDefinition = "text")
    private String content;
    @Column(name = "search_text", columnDefinition = "text")
    private String searchText;
    @Column(name = "chunk_ordinal")
    private int ordinal;
    @Column(columnDefinition = "text")
    private String sourceRefJson;
    @Column(columnDefinition = "text")
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
        String searchText,
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
        this.searchText = searchText;
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

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
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
