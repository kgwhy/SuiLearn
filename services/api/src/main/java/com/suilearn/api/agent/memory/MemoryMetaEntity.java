package com.suilearn.api.agent.memory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "memory_meta")
public class MemoryMetaEntity {
    @Id private String id;
    private String learnerId;
    private String surface;
    private String lastSeenEntityKey;
    private Instant updatedAt;
    protected MemoryMetaEntity() {}
    public MemoryMetaEntity(String id, String learnerId, String surface, String lastSeenEntityKey, Instant updatedAt) {
        this.id=id; this.learnerId=learnerId; this.surface=surface; this.lastSeenEntityKey=lastSeenEntityKey; this.updatedAt=updatedAt;
    }
    public String getId(){return id;} public String getLearnerId(){return learnerId;}
    public String getSurface(){return surface;} public String getLastSeenEntityKey(){return lastSeenEntityKey;}
    public Instant getUpdatedAt(){return updatedAt;}
}
