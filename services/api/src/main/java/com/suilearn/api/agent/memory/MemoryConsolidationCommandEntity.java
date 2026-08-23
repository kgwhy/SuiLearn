package com.suilearn.api.agent.memory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "memory_consolidation_command", uniqueConstraints =
    @UniqueConstraint(name = "uk_memory_consolidation_command_idem", columnNames = "idempotency_key"))
public class MemoryConsolidationCommandEntity {
    @Id private String id;
    private String learnerId;
    private String surface;
    private String operationKey;
    private String idempotencyKey;
    private String status;
    private Instant createdAt;
    private Instant processedAt;
    protected MemoryConsolidationCommandEntity() {}
    public MemoryConsolidationCommandEntity(String id, String learnerId, String surface, String operationKey,
                                            String idempotencyKey, String status, Instant createdAt, Instant processedAt) {
        this.id=id; this.learnerId=learnerId; this.surface=surface; this.operationKey=operationKey;
        this.idempotencyKey=idempotencyKey; this.status=status; this.createdAt=createdAt; this.processedAt=processedAt;
    }
    public String getId(){return id;} public String getLearnerId(){return learnerId;}
    public String getSurface(){return surface;} public String getOperationKey(){return operationKey;}
    public String getIdempotencyKey(){return idempotencyKey;} public String getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;} public Instant getProcessedAt(){return processedAt;}
    public void markProcessed(Instant time){this.status="PROCESSED"; this.processedAt=time;}
}
