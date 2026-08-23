package com.suilearn.api.rag.index;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "index_versions")
public class IndexVersionEntity {
    @Id private String id;
    private String knowledgeBaseId;
    private String signature;
    private long versionNo;
    private String storageRef;
    private boolean ready;
    private Instant createdAt;
    protected IndexVersionEntity() {}
    public IndexVersionEntity(String id, String knowledgeBaseId, String signature, long versionNo,
                              String storageRef, boolean ready, Instant createdAt) {
        this.id=id; this.knowledgeBaseId=knowledgeBaseId; this.signature=signature; this.versionNo=versionNo;
        this.storageRef=storageRef; this.ready=ready; this.createdAt=createdAt;
    }
    public String getId(){return id;} public String getKnowledgeBaseId(){return knowledgeBaseId;}
    public String getSignature(){return signature;} public long getVersionNo(){return versionNo;}
    public String getStorageRef(){return storageRef;} public boolean isReady(){return ready;}
    public Instant getCreatedAt(){return createdAt;}
}
