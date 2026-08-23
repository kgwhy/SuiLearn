package com.suilearn.api.agent.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "memory_l2_doc")
public class MemoryL2DocEntity {
    @Id private String id;
    private String learnerId;
    private String surface;
    @Column(columnDefinition = "text") private String contentMd;
    private String sourceRef;
    private Instant updatedAt;
    protected MemoryL2DocEntity() {}
    public MemoryL2DocEntity(String id, String learnerId, String surface, String contentMd,
                             String sourceRef, Instant updatedAt) {
        this.id=id; this.learnerId=learnerId; this.surface=surface; this.contentMd=contentMd;
        this.sourceRef=sourceRef; this.updatedAt=updatedAt;
    }
    public String getId(){return id;} public String getLearnerId(){return learnerId;}
    public String getSurface(){return surface;} public String getContentMd(){return contentMd;}
    public String getSourceRef(){return sourceRef;} public Instant getUpdatedAt(){return updatedAt;}
}
