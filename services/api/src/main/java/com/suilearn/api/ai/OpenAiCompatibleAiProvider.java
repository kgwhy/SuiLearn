package com.suilearn.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "suilearn.ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleAiProvider implements AiProvider {
    private static final String SYSTEM_PROMPT = """
        You are SuiLearn's study content generator. Return only valid JSON. Do not include markdown fences.
        Generated content must be source-grounded, concise, and safe for user review before saving.
        """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SuiLearnAiProperties properties;

    public OpenAiCompatibleAiProvider(SuiLearnAiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .build());
    }

    OpenAiCompatibleAiProvider(
        SuiLearnAiProperties properties,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public GeneratedQuestion generateQuestion(QuestionGenerationPrompt prompt) {
        var root = completeJson("""
            Return a JSON object with fields:
            questionType, categoryId, categoryName, knowledgePointIds, stem, options, answer, explanation.
            questionType must be one of SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER.
            options and answer must be JSON arrays of strings.
        """, payload()
            .putValue("task", "generate_question")
            .putValue("knowledgeBaseId", prompt.knowledgeBaseId())
            .putValue("sourceType", prompt.sourceType())
            .putValue("sourceId", prompt.sourceId())
            .putValue("requestedQuestionType", prompt.questionType())
            .putValue("categoryId", prompt.categoryId())
            .putValue("categoryName", prompt.categoryName())
            .putValue("knowledgePointIds", prompt.knowledgePointIds() == null ? List.of() : prompt.knowledgePointIds())
            .putValue("sourceRefs", sourceRefs(prompt.sourceRefs()))
            .putValue("userPrompt", prompt.userPrompt())
            .toMap());
        var requestedType = prompt.questionType() == null ? QuestionType.SINGLE_CHOICE : prompt.questionType();
        var questionType = enumValue(root.path("questionType").asText(requestedType.name()), QuestionType.class, requestedType);
        return new GeneratedQuestion(
            questionType,
            textOrDefault(root, "categoryId", prompt.categoryId()),
            textOrDefault(root, "categoryName", prompt.categoryName()),
            strings(root.path("knowledgePointIds"), prompt.knowledgePointIds()),
            requiredText(root, "stem"),
            strings(root.path("options"), List.of()),
            strings(root.path("answer"), List.of()),
            requiredText(root, "explanation")
        );
    }

    @Override
    public GeneratedNote generateKnowledgePointExplanation(KnowledgePointExplanationPrompt prompt) {
        var root = completeJson("""
            Return a JSON object with fields: title, content.
            The content must explain the knowledge point using the provided source references.
        """, payload()
            .putValue("task", "generate_knowledge_point_explanation")
            .putValue("knowledgeBaseId", prompt.knowledgeBaseId())
            .putValue("knowledgePointId", prompt.knowledgePointId())
            .putValue("knowledgePointName", prompt.knowledgePointName())
            .putValue("knowledgePointDescription", prompt.knowledgePointDescription())
            .putValue("sourceRefs", sourceRefs(prompt.sourceRefs()))
            .putValue("userPrompt", prompt.userPrompt())
            .toMap());
        return new GeneratedNote(requiredText(root, "title"), requiredText(root, "content"));
    }

    @Override
    public GeneratedNote generateReviewSuggestion(ReviewSuggestionPrompt prompt) {
        var root = completeJson("""
            Return a JSON object with fields: title, content.
            The content must give concrete review suggestions for the weak points or wrong questions.
        """, payload()
            .putValue("task", "generate_review_suggestion")
            .putValue("knowledgeBaseId", prompt.knowledgeBaseId())
            .putValue("sourceRefs", sourceRefs(prompt.sourceRefs()))
            .putValue("weakKnowledgePointIds", prompt.weakKnowledgePointIds() == null ? List.of() : prompt.weakKnowledgePointIds())
            .putValue("wrongQuestionIds", prompt.wrongQuestionIds() == null ? List.of() : prompt.wrongQuestionIds())
            .putValue("userPrompt", prompt.userPrompt())
            .toMap());
        return new GeneratedNote(requiredText(root, "title"), requiredText(root, "content"));
    }

    private JsonNode completeJson(String instruction, Map<String, Object> payload) {
        ensureConfigured();
        var body = requestBody(instruction, payload);
        var uri = URI.create(normalizeBaseUrl(properties.baseUrl()) + "/chat/completions");
        var request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .header("Authorization", "Bearer " + properties.apiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        RuntimeException lastFailure = null;
        for (var attempt = 0; attempt <= Math.max(0, properties.maxRetries()); attempt++) {
            try {
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseAssistantJson(response.body());
                }
                lastFailure = new IllegalStateException("OpenAI-compatible provider returned HTTP " + response.statusCode());
            } catch (IOException exception) {
                lastFailure = new IllegalStateException("OpenAI-compatible provider request failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("OpenAI-compatible provider request interrupted", exception);
            }
        }
        throw lastFailure == null ? new IllegalStateException("OpenAI-compatible provider request failed") : lastFailure;
    }

    private String requestBody(String instruction, Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "model", properties.chatModel(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", instruction + "\nInput JSON:\n" + objectMapper.writeValueAsString(payload))
                )
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize OpenAI-compatible request", exception);
        }
    }

    private JsonNode parseAssistantJson(String responseBody) {
        try {
            var root = objectMapper.readTree(responseBody);
            var content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new IllegalStateException("OpenAI-compatible response did not include assistant content");
            }
            return objectMapper.readTree(stripJsonFence(content));
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI-compatible response was not valid JSON", exception);
        }
    }

    private List<Map<String, Object>> sourceRefs(List<SourceRef> sourceRefs) {
        if (sourceRefs == null) {
            return List.of();
        }
        return sourceRefs.stream()
            .map(ref -> Map.<String, Object>of(
                "type", ref.type(),
                "id", ref.id(),
                "knowledgeBaseId", valueOrEmpty(ref.knowledgeBaseId()),
                "title", valueOrEmpty(ref.title()),
                "materialId", valueOrEmpty(ref.materialId()),
                "chunkId", valueOrEmpty(ref.chunkId()),
                "excerpt", valueOrEmpty(ref.excerpt())
            ))
            .toList();
    }

    private List<String> strings(JsonNode node, List<String> fallback) {
        if (!node.isArray()) {
            return fallback == null ? List.of() : fallback;
        }
        var values = new ArrayList<String>();
        node.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) {
                values.add(value.asText());
            }
        });
        return values;
    }

    private String requiredText(JsonNode root, String field) {
        var value = root.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalStateException("OpenAI-compatible provider returned blank " + field);
        }
        return value;
    }

    private String textOrDefault(JsonNode root, String field, String fallback) {
        var value = root.path(field).asText("");
        return value.isBlank() ? fallback : value;
    }

    private <E extends Enum<E>> E enumValue(String value, Class<E> enumType, E fallback) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private void ensureConfigured() {
        if (!properties.hasOpenAiCompatibleConfiguration()) {
            throw new IllegalStateException("OpenAI-compatible provider is missing baseUrl, apiKey, chatModel, or embeddingModel");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        var normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }

    private String stripJsonFence(String content) {
        var trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            var firstNewline = trimmed.indexOf('\n');
            var lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private Object valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private PayloadBuilder payload() {
        return new PayloadBuilder();
    }

    private static class PayloadBuilder {
        private final Map<String, Object> values = new LinkedHashMap<>();

        PayloadBuilder putValue(String key, Object value) {
            values.put(key, value);
            return this;
        }

        Map<String, Object> toMap() {
            return values;
        }
    }
}
