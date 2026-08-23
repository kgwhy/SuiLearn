package com.suilearn.api.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.tool.ToolDefinition;
import com.suilearn.api.agent.tool.ToolResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentTurnRuntimeTypesTest {
    @Test
    void scopeRequiresAtLeastOneBoundary() {
        assertThat(new StudyScope("kb", null).knowledgeBaseId()).isEqualTo("kb");
        assertThat(new StudyScope(null, "mat").materialId()).isEqualTo("mat");
        assertThatThrownBy(() -> new StudyScope("", " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void streamEventIsValidatedAndImmutable() {
        var event = new StreamEvent("turn", "sess", 1, EventType.TURN_STARTED, "study_agent", null, "",
            Map.of("k", "v"), Instant.EPOCH);
        assertThat(event.metadata()).containsEntry("k", "v");
        assertThatThrownBy(() -> event.metadata().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new StreamEvent("", "sess", 1, EventType.DONE, null, null, "", Map.of(), Instant.EPOCH))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(EventType.DONE.isTerminal()).isTrue();
        assertThat(EventType.CONTENT.isTerminal()).isFalse();
        assertThat(TurnStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(TurnStatus.RUNNING.isActive()).isTrue();
    }

    @Test
    void turnContextDefaultsCapabilityAndCopiesCollections() {
        var context = new TurnContext("turn", "sess", "learner", null, new StudyScope("kb", null), List.of(),
            "hello", List.of(), List.of(), Map.of());
        assertThat(context.capability()).isEqualTo(TurnContext.DEFAULT_CAPABILITY);
        assertThat(context.sources()).isEmpty();
        assertThat(context.attachments()).isEmpty();
    }

    @Test
    void capabilityAndToolProtocolsHaveStableShapes() {
        var manifest = new CapabilityManifest("study_agent", "Study agent", Set.of("search_knowledge"));
        var definition = new ToolDefinition("search_knowledge", "Search", Map.of("type", "object"),
            false, Set.of("kb"));
        var result = new ToolResult("ok", List.of(), Map.of("attempts", 1), true, null);
        assertThat(manifest.ownedTools()).contains("search_knowledge");
        assertThat(definition.requiredScopes()).contains("kb");
        assertThat(result.success()).isTrue();
        assertThat(result.pauseForUser()).isNull();
    }
}
