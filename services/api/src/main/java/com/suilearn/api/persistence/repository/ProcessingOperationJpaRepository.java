package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.ProcessingOperationEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessingOperationJpaRepository extends JpaRepository<ProcessingOperationEntity, String> {
    Optional<ProcessingOperationEntity> findByOperationKey(String operationKey);
    List<ProcessingOperationEntity> findByState(String state);

    @Modifying
    @Query(value = """
        insert into processing_operations
          (id, operation_key, task_id, stage, state, attempt_count, adapter_version, created_at, updated_at, started_at, lease_expires_at)
        values
          (:id, :operationKey, :taskId, :stage, 'STARTED', 1, :adapterVersion, :now, :now, :now, :leaseExpiresAt)
        on conflict (operation_key) do nothing
        """, nativeQuery = true)
    int insertStartedIfAbsent(
        @Param("id") String id, @Param("operationKey") String operationKey, @Param("taskId") String taskId,
        @Param("stage") String stage, @Param("adapterVersion") String adapterVersion, @Param("now") Instant now,
        @Param("leaseExpiresAt") Instant leaseExpiresAt
    );

    @Modifying
    @Query(value = """
        update processing_operations
        set state = 'STARTED', attempt_count = coalesce(attempt_count, 0) + 1, started_at = :now,
            lease_expires_at = :leaseExpiresAt, updated_at = :now
        where operation_key = :operationKey and state = 'RETRYABLE'
        """, nativeQuery = true)
    int restartRetryable(@Param("operationKey") String operationKey, @Param("now") Instant now,
                         @Param("leaseExpiresAt") Instant leaseExpiresAt);

    @Modifying
    @Query(value = """
        update processing_operations
        set state = 'STARTED', attempt_count = coalesce(attempt_count, 0) + 1, started_at = :now,
            lease_expires_at = :leaseExpiresAt, updated_at = :now
        where operation_key = :operationKey and state = 'STARTED'
          and (lease_expires_at is null or lease_expires_at <= :now)
        """, nativeQuery = true)
    int reclaimExpiredStarted(@Param("operationKey") String operationKey, @Param("now") Instant now,
                              @Param("leaseExpiresAt") Instant leaseExpiresAt);
}
