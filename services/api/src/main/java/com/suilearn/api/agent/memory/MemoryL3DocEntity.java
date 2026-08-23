package com.suilearn.api.agent.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "memory_l3_doc")
public class MemoryL3DocEntity {
    @Id private String id;
    private String learnerId;
    private String slot;
    @Column(columnDefinition = "text") private String contentMd;
    private Instant updatedAt;
    protected MemoryL3DocEntity() {}
    public MemoryL3DocEntity(String id, String learnerId, String slot, String contentMd, Instant updatedAt) {
        this.id=id; this.learnerId=learnerId; this.slot=slot; this.contentMd=contentMd; this.updatedAt=updatedAt;
    }
    public String getId(){return id;} public String getLearnerId(){return learnerId;}
    public String getSlot(){return slot;} public String getContentMd(){return contentMd;}
    public Instant getUpdatedAt(){return updatedAt;}
}
