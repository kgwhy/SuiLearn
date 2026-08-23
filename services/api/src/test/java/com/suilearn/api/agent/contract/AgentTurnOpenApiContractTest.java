package com.suilearn.api.agent.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class AgentTurnOpenApiContractTest {
    @Test
    @SuppressWarnings("unchecked")
    void v2TurnPathsAndSchemasAreAdditive() throws Exception {
        String contract = Files.readString(Path.of("..", "..", "contracts", "openapi", "suilearn-v2.yaml"));
        Map<String, Object> document = new Yaml().load(contract);
        Map<String, Object> paths = map(document.get("paths"));
        assertThat(paths.keySet()).contains(
            "/api/v2/agent/turns",
            "/api/v2/agent/turns/{turnId}/events",
            "/api/v2/agent/turns/{turnId}/cancel",
            "/api/v2/agent/turns/{turnId}/reply",
            "/api/v2/agent/sessions/{sessionId}/active-turn"
        );
        assertThat(paths.keySet()).doesNotContain("/api/v2/agents/study/runs");

        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        Map<String, Object> request = map(schemas.get("StartAgentTurnRequest"));
        assertThat((List<String>) request.get("required")).containsExactlyInAnyOrder("learnerId", "message", "scope");

        Map<String, Object> event = map(schemas.get("AgentTurnEvent"));
        assertThat((List<String>) event.get("required")).contains("turnId", "sessionId", "seq", "type", "metadata");

        Map<String, Object> result = map(schemas.get("AgentTurnResult"));
        assertThat((List<String>) result.get("required")).contains(
            "promptTokens", "completionTokens", "usageCostUsd", "actionTraceCount", "estimatedContextTokens");
        assertThat(map(result.get("properties")).keySet()).contains(
            "promptTokens", "completionTokens", "usageCostUsd", "actionTraceCount", "estimatedContextTokens");

        assertThat(contract).contains("AGENT_TURN_NOT_WAITING_FOR_INPUT", "TURN_EXECUTOR_UNAVAILABLE");
        assertThat(contract).doesNotContain("StudyAgentRunRequest", "StudyAgentError");
        assertThat(contract).contains("FAILED_ORPHANED", "turn_started");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
