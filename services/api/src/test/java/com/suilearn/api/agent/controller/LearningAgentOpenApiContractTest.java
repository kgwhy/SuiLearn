package com.suilearn.api.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.yaml.snakeyaml.Yaml;

class LearningAgentOpenApiContractTest {
    @Test
    void controllerPathsAndDtosMatchTheAdditiveOpenApiSurface() throws Exception {
        assertThat(LearningAgentController.class.getMethod("run", StudyAgentDtos.RunRequest.class)
            .getAnnotation(PostMapping.class).value()).containsExactly("/api/v2/agents/study/runs");
        assertThat(LearningAgentController.class.getMethod("deleteMemories", String.class)
            .getAnnotation(DeleteMapping.class).value())
            .containsExactly("/api/v2/agents/study/learners/{learnerId}/memories");

        String contract = Files.readString(Path.of("..", "..", "contracts", "openapi", "suilearn-v2.yaml"));
        assertThat(contract).contains("StudyAgentRunRequest:", "StudyAgentRunResponse:",
            "StudyAgentMemoryDeletionResponse:", "INVALID_MODEL_OUTPUT", "AGENT_SESSION_MEMORY_UNAVAILABLE");
        assertThat(StudyAgentDtos.RunRequest.class.getRecordComponents())
            .extracting(java.lang.reflect.RecordComponent::getName)
            .containsExactly("learnerId", "sessionId", "question", "knowledgeBaseId", "materialId",
                "practiceCount", "difficulty");
        assertThat(contract).contains("enum: [EASY, MEDIUM, HARD]", "default: MEDIUM");

        Map<String, Object> document = new Yaml().load(contract);
        Map<String, Object> components = map(document.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        Map<String, Object> requestSchema = map(schemas.get("StudyAgentRunRequest"));
        assertThat((List<String>) requestSchema.get("required"))
            .containsExactlyInAnyOrder("learnerId", "question")
            .doesNotContain("sessionId");
        Map<String, Object> responseSchema = map(schemas.get("StudyAgentRunResponse"));
        assertThat((List<String>) responseSchema.get("required")).contains("sessionId");
        Map<String, Object> deletion = map(schemas.get("StudyAgentMemoryLayerDeletion"));
        Map<String, Object> deletionStatus = map(map(deletion.get("properties")).get("status"));
        assertThat((List<String>) deletionStatus.get("enum")).contains("DELETED", "NOT_FOUND", "FAILED");
        Map<String, Object> trace = map(schemas.get("StudyAgentActionTraceEntry"));
        Map<String, Object> traceAction = map(map(trace.get("properties")).get("action"));
        assertThat((List<String>) traceAction.get("enum")).contains("SCHEMA_REPAIR");
    }

    @Test
    void errorAdviceUsesContractStatusesAndNeverEchoesExceptionBodies() {
        var advice = new LearningAgentExceptionHandler();
        assertThat(advice.handle(new AgentApiException(AgentErrorCode.AGENT_FEATURE_DISABLED)).getStatusCode().value())
            .isEqualTo(404);
        assertThat(advice.handle(new AgentApiException(AgentErrorCode.INVALID_MODEL_OUTPUT)).getStatusCode().value())
            .isEqualTo(502);
        var response = advice.handle(new AgentApiException(AgentErrorCode.AGENT_MODEL_UNAVAILABLE));
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().code()).isEqualTo(AgentErrorCode.AGENT_MODEL_UNAVAILABLE);
        assertThat(response.getBody().toString()).doesNotContain("prompt", "raw", "learner", "session");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
