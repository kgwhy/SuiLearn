package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.InboundMessageEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InboundMessageJpaRepository extends JpaRepository<InboundMessageEntity, String> {
    Optional<InboundMessageEntity> findByMessageId(String messageId);

    @Modifying
    @Query(value = """
        insert into inbound_messages (id, message_id, state, created_at)
        values (:id, :messageId, 'CLAIMED', :createdAt)
        on conflict (message_id) do nothing
        """, nativeQuery = true)
    int insertClaimIfAbsent(@Param("id") String id, @Param("messageId") String messageId, @Param("createdAt") Instant createdAt);
}
