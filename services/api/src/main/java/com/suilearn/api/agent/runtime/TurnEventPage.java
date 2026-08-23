package com.suilearn.api.agent.runtime;

import java.util.List;

public record TurnEventPage(String turnId, long afterSeq, long lastSeq, List<StreamEvent> events) {
    public TurnEventPage {
        if (turnId == null || turnId.isBlank()) {
            throw new IllegalArgumentException("turnId is required");
        }
        if (afterSeq < 0 || lastSeq < 0) {
            throw new IllegalArgumentException("afterSeq and lastSeq must be >= 0");
        }
        events = List.copyOf(events == null ? List.of() : events);
    }
}
