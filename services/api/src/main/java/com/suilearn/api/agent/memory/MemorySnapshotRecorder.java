package com.suilearn.api.agent.memory;

import java.time.Clock;
import java.util.UUID;

public final class MemorySnapshotRecorder {
    private final MemorySnapshotRepository snapshots;
    private final Clock clock;

    public MemorySnapshotRecorder(MemorySnapshotRepository snapshots, Clock clock) {
        this.snapshots = snapshots;
        this.clock = clock;
    }

    public boolean record(String learnerId, String surface, String entityKey, String content, String fingerprint) {
        if (snapshots.existsByEntityKeyAndFingerprint(entityKey, fingerprint)) {
            return false;
        }
        snapshots.save(new MemorySnapshotEntity("snap_" + UUID.randomUUID().toString().replace("-", ""),
            learnerId, surface, entityKey, content, fingerprint, clock.instant(), false));
        return true;
    }
}
