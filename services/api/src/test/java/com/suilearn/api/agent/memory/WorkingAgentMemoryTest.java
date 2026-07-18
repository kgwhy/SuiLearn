package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class WorkingAgentMemoryTest {
    @Test
    void releasesRequestStateWhenScopeCloses() {
        WorkingMemory memory = new WorkingMemory();
        memory.put("plan", "research-then-practice");

        assertThat(memory.get("plan")).contains("research-then-practice");

        memory.close();

        assertThat(memory.isReleased()).isTrue();
        assertThatIllegalStateException().isThrownBy(() -> memory.get("plan"));
    }
}
