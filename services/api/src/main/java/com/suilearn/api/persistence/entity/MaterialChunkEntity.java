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
    private String materialId;
    @Lob
    private String content;
    @Column(name = "chunk_ordinal")
    private int ordinal;
    @Lob
    private String sourceRefJson;

    protected MaterialChunkEntity() {
    }

    public MaterialChunkEntity(String id, String materialId, String content, int ordinal, String sourceRefJson) {
        this.id = id;
        this.materialId = materialId;
        this.content = content;
        this.ordinal = ordinal;
        this.sourceRefJson = sourceRefJson;
    }

    public String getId() {
        return id;
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
}
