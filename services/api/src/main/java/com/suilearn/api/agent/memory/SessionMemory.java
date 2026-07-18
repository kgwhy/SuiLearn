package com.suilearn.api.agent.memory;

import java.util.List;

public record SessionMemory(List<SessionTurn> turns) {
    public SessionMemory {
        turns = turns == null ? List.of() : List.copyOf(turns);
    }
}
