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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
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
    public List<GeneratedKnowledgePoint> extractKnowledgePoints(KnowledgePointExtractionPrompt prompt) {
        var root = completeJson("""
            Return a JSON object with field knowledgePoints.
            knowledgePoints must be an array of objects with fields: name, description.
            Extract only real study concepts, APIs, patterns, or pitfalls that are supported by the source excerpts.
            Do not return sentence fragments, headings without concept value, punctuation-only text, or duplicate names.
        """, payload()
            .putValue("task", "extract_knowledge_points")
            .putValue("knowledgeBaseId", prompt.knowledgeBaseId())
            .putValue("materialId", prompt.materialId())
            .putValue("materialTitle", prompt.materialTitle())
            .putValue("maxKnowledgePoints", prompt.maxKnowledgePoints())
            .putValue("sourceRefs", sourceRefs(prompt.evidenceRefs()))
            .toMap());
        return knowledgePoints(root.path("knowledgePoints"), prompt.maxKnowledgePoints());
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

    @Override
    public GeneratedAnswer answerQuestion(AnswerQuestionPrompt prompt) {
        var root = completeJson("""
            Return a JSON object with fields: answer, uncertain.
            Prefer the provided evidence content over sourceRef excerpts.
            Also return statements as an array of objects with fields: text, citations.
            Answer the user's question using only the provided evidence content.
            Cite supporting evidence inline with bracketed numbers like [1], [2], matching citationNumber.
            If the evidence does not contain enough support, set uncertain to true and start answer with "\u4e0d\u786e\u5b9a\uff1a".
            If the excerpts do not contain enough evidence, set uncertain to true and start answer with "不确定：".
            Do not use outside knowledge, do not invent facts, and do not cite sources that are not provided.
        """, payload()
            .putValue("task", "answer_question")
            .putValue("knowledgeBaseId", prompt.knowledgeBaseId())
            .putValue("materialId", prompt.materialId())
            .putValue("question", prompt.question())
            .putValue("evidence", numberedEvidence(prompt))
            .putValue("sourceRefs", numberedSourceRefs(prompt.sourceRefs()))
            .toMap());
        return new GeneratedAnswer(
            requiredText(root, "answer"),
            root.path("uncertain").asBoolean(false),
            statements(root.path("statements"))
        );
    }

    private JsonNode completeJson(String instruction, Map<String, Object> payload) {
        ensureConfigured();
        var body = requestBody(instruction, payload);
        var uri = URI.create(normalizeBaseUrl(properties.effectiveChatBaseUrl()) + "/chat/completions");
        var request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .header("Authorization", "Bearer " + properties.effectiveChatApiKey())
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

    private List<Map<String, Object>> numberedSourceRefs(List<SourceRef> sourceRefs) {
        if (sourceRefs == null) {
            return List.of();
        }
        var values = new ArrayList<Map<String, Object>>();
        for (var index = 0; index < sourceRefs.size(); index++) {
            var ref = sourceRefs.get(index);
            values.add(Map.<String, Object>of(
                "citationNumber", index + 1,
                "type", ref.type(),
                "id", ref.id(),
                "knowledgeBaseId", valueOrEmpty(ref.knowledgeBaseId()),
                "title", valueOrEmpty(ref.title()),
                "materialId", valueOrEmpty(ref.materialId()),
                "chunkId", valueOrEmpty(ref.chunkId()),
                "excerpt", valueOrEmpty(ref.excerpt())
            ));
        }
        return values;
    }

    private List<Map<String, Object>> numberedEvidence(AnswerQuestionPrompt prompt) {
        if (prompt.evidence() != null && !prompt.evidence().isEmpty()) {
            return prompt.evidence().stream()
                .map(evidence -> Map.<String, Object>of(
                    "citationNumber", evidence.citationNumber(),
                    "type", evidence.sourceRef().type(),
                    "id", evidence.sourceRef().id(),
                    "knowledgeBaseId", valueOrEmpty(evidence.sourceRef().knowledgeBaseId()),
                    "title", valueOrEmpty(evidence.sourceRef().title()),
                    "materialId", valueOrEmpty(evidence.sourceRef().materialId()),
                    "chunkId", valueOrEmpty(evidence.sourceRef().chunkId()),
                    "excerpt", valueOrEmpty(evidence.sourceRef().excerpt()),
                    "content", valueOrEmpty(evidence.content()),
                    "score", evidence.score()
                ))
                .toList();
        }
        return numberedSourceRefs(prompt.sourceRefs()).stream()
            .map(ref -> {
                Map<String, Object> values = new LinkedHashMap<>(ref);
                values.put("content", values.getOrDefault("excerpt", ""));
                values.put("score", 0.0);
                return values;
            })
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

    private List<GeneratedKnowledgePoint> knowledgePoints(JsonNode node, int maxKnowledgePoints) {
        if (!node.isArray()) {
            return List.of();
        }
        var values = new ArrayList<GeneratedKnowledgePoint>();
        for (var value : node) {
            var name = value.path("name").asText("");
            var description = value.path("description").asText("");
            if (!name.isBlank()) {
                values.add(new GeneratedKnowledgePoint(name, description));
            }
            if (values.size() >= maxKnowledgePoints) {
                break;
            }
        }
        return values;
    }

    private List<GeneratedStatement> statements(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        var values = new ArrayList<GeneratedStatement>();
        for (var value : node) {
            var text = value.path("text").asText("");
            if (!text.isBlank()) {
                values.add(new GeneratedStatement(text, integers(value.path("citations"))));
            }
        }
        return values;
    }

    private List<Integer> integers(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        var values = new ArrayList<Integer>();
        for (var value : node) {
            if (value.canConvertToInt()) {
                values.add(value.asInt());
            }
        }
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
        if (!properties.hasOpenAiCompatibleChatConfiguration()) {
            throw new IllegalStateException("OpenAI-compatible provider is missing chat baseUrl, chat apiKey, or chatModel");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        var normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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
