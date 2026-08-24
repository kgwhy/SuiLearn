package com.suilearn.api.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmRequest;
import com.suilearn.api.agent.llm.LlmResponse;
import com.suilearn.api.agent.llm.LlmUsage;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.retrieval.Retriever;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SmartRetrieverTest {
    @Test
    void rewritesRunsInParallelAndDeduplicates() {
        var queries = new CopyOnWriteArrayList<String>();
        var ids = new AtomicInteger();
        Retriever delegate = new Retriever() {
            @Override public List<SearchResult> search(RetrievalRequest request) {
                queries.add(request.query());
                return List.of(result("id-" + ids.incrementAndGet(), "title", request.query()));
            }
            @Override public List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) { return List.of(); }
        };
        LlmClient llm = new LlmClient() {
            @Override public java.util.stream.Stream<com.suilearn.api.agent.llm.LlmChunk> stream(LlmRequest request) {
                return java.util.stream.Stream.empty();
            }
            @Override public LlmResponse chat(LlmRequest request) {
                return new LlmResponse("[\"hooks basics\",\"useEffect\"]", List.of(), new LlmUsage(2, 2), "stop");
            }
        };
        var retriever = new SmartRetriever(delegate, llm, JsonMapper.builder().findAndAddModules().build(), "fake");

        var results = retriever.search(new Retriever.RetrievalRequest("react hooks", "kb", null));

        assertThat(queries).hasSize(3);
        assertThat(queries).containsExactlyInAnyOrder("react hooks", "hooks basics", "useEffect");
        assertThat(results).extracting(SearchResult::id).containsExactlyInAnyOrder("id-1", "id-2", "id-3");
    }

    private SearchResult result(String id, String title, String summary) {
        return new SearchResult(id, SearchResultType.MATERIAL_CHUNK, title, summary, 0.8, "kb",
            List.of(), List.of());
    }
}
