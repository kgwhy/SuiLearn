package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemoryPersistenceEntitiesTest {
    @Test
    void traceSnapshotAndDocsCarryBoundedShapes() {
        var now = Instant.parse("2026-08-23T08:00:00Z");
        var trace = new MemoryTraceEntity("trace_1", "learner", "turn", "study", "turn_completed", "summary", now);
        var snap = new MemorySnapshotEntity("snap_1", "learner", "answer", "q1", "content", "fp", now, false);
        var l2 = new MemoryL2DocEntity("learner:answer", "learner", "answer", "## answer", "snapshot:answer", now);
        var l3 = new MemoryL3DocEntity("learner:recent", "learner", "recent", "## recent", now);

        assertThat(trace.getKind()).isEqualTo("turn_completed");
        assertThat(snap.isConsumed()).isFalse();
        snap.markConsumed();
        assertThat(snap.isConsumed()).isTrue();
        assertThat(l2.getSurface()).isEqualTo("answer");
        assertThat(l3.getSlot()).isEqualTo("recent");
    }
}
