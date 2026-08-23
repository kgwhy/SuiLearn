package com.suilearn.api.agent.infrastructure.turn;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TurnEventId implements Serializable {
    @Column(name = "turn_id")
    private String turnId;
    private long seq;

    protected TurnEventId() {
    }

    public TurnEventId(String turnId, long seq) {
        this.turnId = turnId;
        this.seq = seq;
    }

    public String getTurnId() { return turnId; }
    public long getSeq() { return seq; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof TurnEventId other)) return false;
        return seq == other.seq && Objects.equals(turnId, other.turnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(turnId, seq);
    }
}
