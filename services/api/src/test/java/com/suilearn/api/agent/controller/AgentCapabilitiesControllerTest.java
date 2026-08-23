package com.suilearn.api.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.runtime.CapabilityRegistry;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.runtime.ToolRegistry;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.agent.tool.AskUserTool;
import com.suilearn.api.agent.tool.GeneratePracticeTool;
import com.suilearn.api.agent.tool.PersistMemoryTool;
import com.suilearn.api.agent.tool.ReadEvidenceTool;
import com.suilearn.api.agent.tool.RecallMemoryTool;
import com.suilearn.api.agent.tool.SearchKnowledgeTool;
import com.suilearn.api.agent.tool.Tool;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class AgentCapabilitiesControllerTest {
    @Test
    void pathMatchesContractAndEnabledRegistryIsEnumerable() throws Exception {
        assertThat(AgentCapabilitiesController.class.getMethod("capabilities")
            .getAnnotation(GetMapping.class).value()).containsExactly("/api/v2/agent/capabilities");

        var controller = new AgentCapabilitiesController(CapabilityRegistry.builtin(), toolRegistry(),
            properties(true));
        var response = controller.capabilities();

        assertThat(response.capabilities()).extracting(CapabilityDtos.CapabilityDescriptor::name)
            .containsExactly("question_generation", "rag_qa", "study_agent");
        assertThat(response.tools()).extracting(CapabilityDtos.ToolDescriptor::name).containsExactlyInAnyOrder(
            "search_knowledge", "read_evidence", "generate_practice", "recall_memory", "persist_memory", "ask_user");
        assertThat(response.tools().getFirst().parameters()).containsKey("type");
    }

    @Test
    void disabledAgentReturnsStableFeatureDisabledError() {
        var controller = new AgentCapabilitiesController(CapabilityRegistry.builtin(), toolRegistry(),
            properties(false));
        assertThatThrownBy(controller::capabilities)
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_FEATURE_DISABLED));
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

    private AgentConfigurationProperties properties(boolean enabled) {
        return new AgentConfigurationProperties(enabled, 4, 3, 8, Duration.ofSeconds(90), 12000, 3,
            new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }
}
