package com.suilearn.api.agent.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "memory_trace")
public class MemoryTraceEntity {
    @Id private String id;
    private String learnerId;
    private String turnId;
    private String surface;
    private String kind;
    @Column(columnDefinition = "text") private String payload;
    private Instant ts;
    protected MemoryTraceEntity() {}
    public MemoryTraceEntity(String id, String learnerId, String turnId, String surface, String kind,
                             String payload, Instant ts) {
        this.id=id; this.learnerId=learnerId; this.turnId=turnId; this.surface=surface; this.kind=kind;
        this.payload=payload; this.ts=ts;
    }
    public String getId(){return id;} public String getLearnerId(){return learnerId;}
    public String getTurnId(){return turnId;} public String getSurface(){return surface;}
    public String getKind(){return kind;} public String getPayload(){return payload;} public Instant getTs(){return ts;}
}
