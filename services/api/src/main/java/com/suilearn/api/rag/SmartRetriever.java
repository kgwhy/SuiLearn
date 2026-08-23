package com.suilearn.api.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.llm.LlmRequest;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.retrieval.Retriever;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;

public final class SmartRetriever implements Retriever {
    private final Retriever delegate;
    private final LlmClient client;
    private final ObjectMapper objectMapper;
    private final String model;

    public SmartRetriever(Retriever delegate, LlmClient client, ObjectMapper objectMapper, String model) {
        this.delegate = delegate;
        this.client = client;
        this.objectMapper = objectMapper;
        this.model = model == null || model.isBlank() ? "suilearn-default" : model;
    }

    @Override
    public List<SearchResult> search(RetrievalRequest request) {
        if (client == null || request == null || request.query() == null || request.query().isBlank()) {
            return delegate.search(request);
        }
        List<String> queries = rewrite(request.query());
        var merged = new LinkedHashMap<String, SearchResult>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = queries.stream().map(query -> executor.submit(() ->
                delegate.search(new RetrievalRequest(query, request.knowledgeBaseId(), request.materialId())))).toList();
            for (var future : futures) {
                for (SearchResult result : future.get()) {
                    merged.putIfAbsent(result.id(), result);
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return delegate.search(request);
        } catch (java.util.concurrent.ExecutionException execution) {
            return delegate.search(request);
        }
        return new ArrayList<>(merged.values());
    }

    @Override
    public List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) {
        return delegate.retrieveEvidence(request, limit);
    }

    private List<String> rewrite(String query) {
        try {
            var response = client.chat(new LlmRequest(model, List.of(
                LlmMessage.system("Return a JSON array of at most 3 concise search queries. Only JSON."),
                LlmMessage.user(query)), List.of(), 0.0, null));
            JsonNode root = objectMapper.readTree(stripFence(response.content()));
            var queries = new ArrayList<String>();
            queries.add(query);
            if (root.isArray()) {
                for (JsonNode item : root) {
                    String text = item.asText("");
                    if (!text.isBlank() && !queries.contains(text) && queries.size() < 3) {
                        queries.add(text);
                    }
                }
            }
            return queries;
        } catch (RuntimeException | java.io.IOException failure) {
            return List.of(query);
        }
    }

    private String stripFence(String content) {
        return content == null ? "" : content.strip().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
    }
}
