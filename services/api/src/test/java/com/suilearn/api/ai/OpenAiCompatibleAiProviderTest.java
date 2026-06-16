package com.suilearn.api.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.retrieval.OpenAiCompatibleEmbeddingProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleAiProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> lastChatRequest = new AtomicReference<>("");
    private final AtomicReference<String> lastEmbeddingRequest = new AtomicReference<>("");
    private HttpServer server;
    private SuiLearnAiProperties properties;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", this::handleChatCompletion);
        server.createContext("/v1/embeddings", this::handleEmbedding);
        server.start();
        properties = new SuiLearnAiProperties(
            "openai-compatible",
            "http://127.0.0.1:" + server.getAddress().getPort(),
            "test-key",
            "test-chat",
            "test-embedding",
            5000,
            0
        );
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void generatesQuestionFromOpenAiCompatibleChatJson() {
        var provider = new OpenAiCompatibleAiProvider(properties, objectMapper);

        var question = provider.generateQuestion(new AiProvider.QuestionGenerationPrompt(
            "kb_1",
            List.of(sourceRef()),
            SourceType.MATERIAL,
            "mat_1",
            QuestionType.SINGLE_CHOICE,
            "java",
            "Java",
            List.of("kp_1"),
            "生成一道题"
        ));

        assertThat(question.questionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(question.stem()).isEqualTo("What does HashMap use for lookup?");
        assertThat(question.options()).containsExactly("A. Hash table", "B. Linked only");
        assertThat(question.answer()).containsExactly("A");
        assertThat(question.explanation()).contains("source");
        assertThat(lastChatRequest.get()).contains("\"response_format\"");
        assertThat(lastChatRequest.get()).contains("HashMap source");
    }

    @Test
    void generatesNoteAndEmbeddingsThroughOpenAiCompatibleHttpApi() {
        var aiProvider = new OpenAiCompatibleAiProvider(properties, objectMapper);
        var embeddingProvider = new OpenAiCompatibleEmbeddingProvider(properties, objectMapper);

        var note = aiProvider.generateReviewSuggestion(new AiProvider.ReviewSuggestionPrompt(
            "kb_1",
            List.of(sourceRef()),
            List.of("kp_1"),
            List.of("q_1"),
            null
        ));
        var embedding = embeddingProvider.embed("HashMap source");

        assertThat(note.title()).isEqualTo("Review HashMap");
        assertThat(note.content()).contains("collision");
        assertThat(embeddingProvider.model()).isEqualTo("test-embedding");
        assertThat(embedding.values()).containsExactly(0.1, 0.2, 0.3);
        assertThat(lastEmbeddingRequest.get()).contains("test-embedding");
    }

    private SourceRef sourceRef() {
        return new SourceRef(
            SourceType.MATERIAL,
            "mat_1",
            "kb_1",
            "HashMap source",
            "mat_1",
            null,
            false,
            "HashMap uses buckets and handles collisions."
        );
    }

    private void handleChatCompletion(HttpExchange exchange) throws IOException {
        lastChatRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        var content = lastChatRequest.get().contains("generate_question")
            ? """
                {"questionType":"SINGLE_CHOICE","categoryId":"java","categoryName":"Java","knowledgePointIds":["kp_1"],"stem":"What does HashMap use for lookup?","options":["A. Hash table","B. Linked only"],"answer":["A"],"explanation":"The answer is grounded in the source."}
                """
            : """
                {"title":"Review HashMap","content":"Revisit HashMap collision handling and redo one focused question."}
                """;
        var body = objectMapper.writeValueAsString(new ChatResponse(content));
        respond(exchange, body);
    }

    private void handleEmbedding(HttpExchange exchange) throws IOException {
        lastEmbeddingRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        respond(exchange, """
            {"data":[{"embedding":[0.1,0.2,0.3]}]}
            """);
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record ChatResponse(List<Choice> choices) {
        ChatResponse(String content) {
            this(List.of(new Choice(new Message(content))));
        }
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
