package com.suilearn.api.agent.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "memory_snapshot")
public class MemorySnapshotEntity {
    @Id private String id;
    private String learnerId;
    private String surface;
    private String entityKey;
    @Column(columnDefinition = "text") private String content;
    private String fingerprint;
    private Instant createdAt;
    private boolean consumed;
    protected MemorySnapshotEntity() {}
    public MemorySnapshotEntity(String id, String learnerId, String surface, String entityKey, String content,
                                String fingerprint, Instant createdAt, boolean consumed) {
        this.id=id; this.learnerId=learnerId; this.surface=surface; this.entityKey=entityKey; this.content=content;
        this.fingerprint=fingerprint; this.createdAt=createdAt; this.consumed=consumed;
    }
    public String getId(){return id;} public String getLearnerId(){return learnerId;}
    public String getSurface(){return surface;} public String getEntityKey(){return entityKey;}
    public String getContent(){return content;} public String getFingerprint(){return fingerprint;}
    public Instant getCreatedAt(){return createdAt;} public boolean isConsumed(){return consumed;}
    public void markConsumed(){this.consumed=true;}
}
