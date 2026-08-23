package com.suilearn.api.agent.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.config.SuiLearnAiProperties;
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
import java.util.stream.Stream;

/**
 * Streaming OpenAI-compatible chat client. It uses the same java.net.http stack as
 * the structured provider but does not depend on Spring AI types.
 */
public final class OpenAiCompatibleLlmClient implements LlmClient {
    private final SuiLearnAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(SuiLearnAiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs()))).build());
    }

    OpenAiCompatibleLlmClient(SuiLearnAiProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Stream<LlmChunk> stream(LlmRequest request) {
        ensureConfigured();
        HttpRequest httpRequest;
        try {
            httpRequest = HttpRequest.newBuilder(URI.create(normalizeBaseUrl(properties.effectiveChatBaseUrl())
                    + "/chat/completions"))
                .timeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
                .header("Authorization", "Bearer " + properties.effectiveChatApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body(request))))
                .build();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize LlmRequest", exception);
        }
        try {
            HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new IllegalStateException("OpenAI-compatible streaming request failed with status "
                    + response.statusCode());
            }
            return response.body()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring(5).strip())
                .filter(data -> !data.isBlank() && !"[DONE]".equals(data))
                .map(this::parseChunk)
                .onClose(response.body()::close);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI-compatible streaming request interrupted", exception);
        }
    }

    private Map<String, Object> body(LlmRequest request) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", request.model());
        body.put("stream", true);
        body.put("messages", request.messages().stream().map(this::message).toList());
        if (!request.tools().isEmpty()) {
            body.put("tools", request.tools());
        }
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            body.put("max_tokens", request.maxTokens());
        }
        return body;
    }

    private Map<String, Object> message(LlmMessage message) {
        var item = new LinkedHashMap<String, Object>();
        item.put("role", message.role());
        item.put("content", message.content());
        if (message.toolCallId() != null) {
            item.put("tool_call_id", message.toolCallId());
        }
        if (!message.toolCalls().isEmpty()) {
            item.put("tool_calls", message.toolCalls().stream().map(call -> Map.of(
                "id", call.id(), "type", "function",
                "function", Map.of("name", call.name(), "arguments", call.arguments()))).toList());
        }
        return item;
    }

    private LlmChunk parseChunk(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choice = root.path("choices").path(0);
            JsonNode delta = choice.path("delta");
            String content = delta.path("content").asText("");
            var toolDeltas = new ArrayList<LlmToolCallDelta>();
            for (JsonNode call : delta.path("tool_calls")) {
                JsonNode function = call.path("function");
                toolDeltas.add(new LlmToolCallDelta(call.path("index").asInt(0),
                    call.path("id").asText(null), function.path("name").asText(null),
                    function.path("arguments").asText("")));
            }
            JsonNode usageNode = root.hasNonNull("usage") ? root.path("usage") : choice.path("usage");
            LlmUsage usage = usageNode.isMissingNode() || usageNode.isNull()
                ? null
                : new LlmUsage(usageNode.path("prompt_tokens").asLong(0),
                    usageNode.path("completion_tokens").asLong(0));
            return new LlmChunk(content, toolDeltas, usage, choice.path("finish_reason").asText(null));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse OpenAI-compatible streaming chunk", exception);
        }
    }

    private void ensureConfigured() {
        if (!properties.hasOpenAiCompatibleChatConfiguration()) {
            throw new IllegalStateException("OpenAI-compatible provider is missing chat baseUrl, chat apiKey, or chatModel");
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.strip();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
