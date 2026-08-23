package com.suilearn.api.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.llm.LlmRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/** PracticeModelPort backed by the new LlmClient; no Spring AI ChatModel dependency. */
public final class LlmPracticeModelPort implements PracticeModelPort {
    private static final String PROMPT_RESOURCE = "agents/practice-coach/v1/system.md";

    private final LlmClient client;
    private final ObjectMapper objectMapper;
    private final String model;

    public LlmPracticeModelPort(LlmClient client, ObjectMapper objectMapper, String model) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.model = model == null || model.isBlank() ? "suilearn-default" : model;
    }

    @Override
    public Draft generate(Request request) {
        try {
            String system = new String(new ClassPathResource(PROMPT_RESOURCE).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
            String user = objectMapper.writeValueAsString(java.util.Map.of(
                "learningGoal", request.learningGoal(),
                "difficulty", request.difficulty().name(),
                "practiceCount", request.practiceCount(),
                "evidence", request.evidence().items().stream().map(item -> java.util.Map.of(
                    "stableId", item.stableId(), "sourceRef", item.sourceRef(), "content", item.content(),
                    "relevance", item.relevance())).toList()));
            var response = client.chat(new LlmRequest(model, List.of(LlmMessage.system(system),
                LlmMessage.user(user)), List.of(), 0.2, null));
            JsonNode root = objectMapper.readTree(stripFence(response.content()));
            var exercises = new ArrayList<TemporaryExercise>();
            for (JsonNode item : root.path("exercises")) {
                exercises.add(new TemporaryExercise(item.path("question").asText(),
                    item.path("answer").asText(), item.path("explanation").asText(),
                    strings(item.path("citations"))));
            }
            return new Draft(required(root, "explanation"), exercises, strings(root.path("citations")),
                root.path("nextStep").asText(""));
        } catch (IOException exception) {
            throw new IllegalStateException("Practice model prompt or response is unavailable", exception);
        }
    }

    private String required(JsonNode root, String field) {
        String value = root.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalStateException("practice model output is missing " + field);
        }
        return value;
    }

    private List<String> strings(JsonNode node) {
        var values = new ArrayList<String>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private String stripFence(String content) {
        return content == null ? "" : content.strip()
            .replaceFirst("^```(?:json)?\\s*", "")
            .replaceFirst("\\s*```$", "");
    }
}
