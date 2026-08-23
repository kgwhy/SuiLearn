package com.suilearn.api.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.capability.Capability;
import com.suilearn.api.agent.tool.AskUserTool;
import com.suilearn.api.agent.tool.ForbiddenAgentActionException;
import com.suilearn.api.agent.tool.GeneratePracticeTool;
import com.suilearn.api.agent.tool.PersistMemoryTool;
import com.suilearn.api.agent.tool.ReadEvidenceTool;
import com.suilearn.api.agent.tool.RecallMemoryTool;
import com.suilearn.api.agent.tool.SearchKnowledgeTool;
import com.suilearn.api.agent.tool.Tool;
import com.suilearn.api.agent.tool.ToolDefinition;
import com.suilearn.api.agent.tool.ToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CapabilityToolRegistryTest {
    @Test
    void builtinRegistryDefaultsAndRejectsUnknown() {
        var registry = CapabilityRegistry.builtin();
        assertThat(registry.resolve((String) null).manifest().name()).isEqualTo("study_agent");
        assertThat(registry.resolve(" ").manifest().name()).isEqualTo("study_agent");
        assertThat(registry.resolve("rag_qa").manifest().name()).isEqualTo("rag_qa");
        assertThat(registry.manifests()).extracting(manifest -> manifest.name())
            .containsExactly("question_generation", "rag_qa", "study_agent");
        assertThatThrownBy(() -> registry.resolve("missing"))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_CAPABILITY_UNKNOWN));
    }

    @Test
    void toolRegistryComputesOwnedToolsAndRejectsEscalation() {
        var registry = toolRegistry();
        assertThat(registry.openAiSchemas()).hasSize(6);
        assertThat(registry.openAiSchemas().getFirst().keySet()).containsExactlyInAnyOrder("type", "function");

        var study = BuiltinCapabilities.studyAgent().manifest();
        assertThat(registry.allowedTools(study)).containsExactlyInAnyOrder(
            "search_knowledge", "read_evidence", "generate_practice", "recall_memory", "persist_memory", "ask_user");
        var rag = BuiltinCapabilities.ragQa().manifest();
        assertThat(registry.allowedTools(rag)).containsExactlyInAnyOrder("search_knowledge", "read_evidence");

        assertThat(registry.require(study, "search_knowledge").definition().name()).isEqualTo("search_knowledge");
        assertThatThrownBy(() -> registry.require(rag, "persist_memory"))
            .isInstanceOf(ForbiddenAgentActionException.class);
    }

    @Test
    void duplicateToolNamesFailFast() {
        var tools = new LinkedHashMap<String, Tool>();
        tools.put("a", new SearchKnowledgeTool(null));
        tools.put("b", new ReadEvidenceTool(null));
        // search_knowledge and read_evidence names are distinct; create an alias whose definition collides.
        tools.put("c", new Tool() {
            @Override
            public ToolDefinition definition() {
                return new SearchKnowledgeTool(null).definition();
            }

            @Override
            public ToolResult execute(TurnContext context, Map<String, Object> args) {
                return new ToolResult("", java.util.List.of(), Map.of(), true, null);
            }
        });
        assertThatThrownBy(() -> new ToolRegistry(tools)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicate tool name");
    }

    private ToolRegistry toolRegistry() {
        var tools = new LinkedHashMap<String, Tool>();
        for (Tool tool : java.util.List.<Tool>of(new SearchKnowledgeTool(null), new ReadEvidenceTool(null),
            new GeneratePracticeTool(null), new RecallMemoryTool(null), new PersistMemoryTool(null),
            new AskUserTool())) {
            tools.put(tool.definition().name(), tool);
        }
        return new ToolRegistry(tools);
    }
}
