package com.suilearn.api.agent.memory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum MemoryType {
    GOAL,
    PREFERENCE,
    WEAKNESS,
    MASTERY;

    public static Set<MemoryType> allowed() {
        return Collections.unmodifiableSet(EnumSet.allOf(MemoryType.class));
    }
}
