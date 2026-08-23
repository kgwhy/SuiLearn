package com.suilearn.api.agent.memory;

import java.time.Clock;
import java.util.UUID;

public final class MemoryTraceRecorder {
    private final MemoryTraceRepository traces;
    private final Clock clock;

    public MemoryTraceRecorder(MemoryTraceRepository traces, Clock clock) {
        this.traces = traces;
        this.clock = clock;
    }

    public MemoryTraceEntity append(String learnerId, String turnId, String surface, String kind, String payload) {
        return traces.save(new MemoryTraceEntity(newId(), learnerId, turnId, surface, kind, payload, clock.instant()));
    }

    private String newId() { return "trace_" + UUID.randomUUID().toString().replace("-", ""); }
}
