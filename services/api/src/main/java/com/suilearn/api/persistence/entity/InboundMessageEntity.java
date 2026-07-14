package com.suilearn.api.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "inbound_messages", uniqueConstraints = @UniqueConstraint(columnNames = "messageId"))
public class InboundMessageEntity {
    @Id private String id;
    private String messageId;
    private String state;
    private Instant createdAt;
    private Instant completedAt;

    protected InboundMessageEntity() { }

    public static InboundMessageEntity claimed(String id, String messageId, Instant createdAt) {
        var message = new InboundMessageEntity();
        message.id = id;
        message.messageId = messageId;
        message.state = "CLAIMED";
        message.createdAt = createdAt;
        return message;
    }

    public void complete(Instant completedAt) {
        this.state = "COMPLETED";
        this.completedAt = completedAt;
    }
}
