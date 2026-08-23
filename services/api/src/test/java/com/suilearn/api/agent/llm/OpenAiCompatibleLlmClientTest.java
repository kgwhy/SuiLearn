package com.suilearn.api.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleLlmClientTest {
    @Test
    void aggregatesSseContentToolCallsAndUsage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            String body = """
                data: {"choices":[{"delta":{"content":"answer"},"finish_reason":null}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","function":{"name":"search_knowledge","arguments":"{\\"query\\":"}}]},"finish_reason":"tool_calls"}]}

                data: {"choices":[{"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":12,"completion_tokens":7}}

                data: [DONE]

                """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            var properties = properties("http://127.0.0.1:" + port + "/v1");
            var client = new OpenAiCompatibleLlmClient(properties, JsonMapper.builder().findAndAddModules().build(),
                HttpClient.newHttpClient());
            var response = client.chat(new LlmRequest("fake-model",
                List.of(LlmMessage.user("question")), List.of(), 0.2, null));

            assertThat(response.content()).isEqualTo("answer");
            assertThat(response.toolCalls()).singleElement().satisfies(call -> {
                assertThat(call.id()).isEqualTo("call_1");
                assertThat(call.name()).isEqualTo("search_knowledge");
                assertThat(call.arguments()).isEqualTo("{\"query\":");
            });
            assertThat(response.usage().promptTokens()).isEqualTo(12);
            assertThat(response.usage().completionTokens()).isEqualTo(7);
            assertThat(response.finishReason()).isEqualTo("stop");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void non2xxFailsWithoutPartialSuccess() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            var client = new OpenAiCompatibleLlmClient(
                properties("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
                JsonMapper.builder().findAndAddModules().build(), HttpClient.newHttpClient());
            assertThatThrownBy(() -> client.chat(new LlmRequest("fake-model",
                List.of(LlmMessage.user("q")), List.of(), null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("status 500");
        } finally {
            server.stop(0);
        }
    }

    private SuiLearnAiProperties properties(String baseUrl) {
        return new SuiLearnAiProperties("openai-compatible", baseUrl, "test-key", baseUrl, "test-key",
            "", "", "fake-model", "", 5000, 0, 10, 50, 5, 60000, 2);
    }
}
