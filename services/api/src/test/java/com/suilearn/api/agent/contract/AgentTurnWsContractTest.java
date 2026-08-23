package com.suilearn.api.agent.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class AgentTurnWsContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void companionSchemaDeclaresEnvelopeAndCommandSurface() throws Exception {
        String contract = Files.readString(Path.of("..", "..", "contracts", "schemas", "suilearn-ws.yaml"));
        Map<String, Object> document = new Yaml().load(contract);
        Map<String, Object> components = map(document.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        Map<String, Object> envelope = map(schemas.get("TurnWsEnvelope"));
        assertThat((List<Map<String, Object>>) envelope.get("oneOf")).hasSize(5);

        Map<String, Object> command = map(schemas.get("TurnCommandName"));
        assertThat((List<String>) command.get("enum")).contains("start_turn", "subscribe_turn", "resume_from",
            "cancel_turn", "submit_user_reply", "check_active_turn", "ping");

        Map<String, Object> eventType = map(schemas.get("TurnEventType"));
        assertThat((List<String>) eventType.get("enum")).contains("turn_started", "done", "cancelled", "failed",
            "wait_for_input");

        Map<String, Object> errors = map(schemas.get("TurnErrorCode"));
        assertThat((List<String>) errors.get("enum")).contains("AGENT_WEBSOCKET_DISABLED", "AGENT_TURN_ACTIVE_CONFLICT");
    }

    @Test
    void goldenFilesMatchEnvelopeShapes() throws Exception {
        JsonNode start = objectMapper.readTree(readGolden("start-turn-command.json"));
        assertThat(start.get("kind").asText()).isEqualTo("command");
        assertThat(start.get("command").asText()).isEqualTo("start_turn");
        assertThat(start.path("scope").path("knowledgeBaseId").asText()).isEqualTo("kb_01");

        JsonNode subscribe = objectMapper.readTree(readGolden("subscribe-turn-command.json"));
        assertThat(subscribe.get("afterSeq").asInt()).isEqualTo(3);

        JsonNode event = objectMapper.readTree(readGolden("turn-started-event.json"));
        assertThat(event.get("seq").asInt()).isEqualTo(1);
        assertThat(event.get("type").asText()).isEqualTo("turn_started");

        JsonNode error = objectMapper.readTree(readGolden("unavailable-error.json"));
        assertThat(error.get("code").asText()).isEqualTo("TURN_EXECUTOR_UNAVAILABLE");
        assertThat(error.get("message").asText()).doesNotContain("prompt", "api", "learner_01");
    }

    private String readGolden(String name) throws Exception {
        return Files.readString(Path.of("src", "test", "resources", "agent-turn", "golden", name));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
