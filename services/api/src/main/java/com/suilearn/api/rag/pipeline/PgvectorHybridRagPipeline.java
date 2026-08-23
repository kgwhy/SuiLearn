package com.suilearn.api.rag.pipeline;

import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.retrieval.Retriever;
import java.util.List;

public final class PgvectorHybridRagPipeline implements RagPipeline {
    public static final String NAME = "pgvector-hybrid";

    private final Retriever retriever;

    public PgvectorHybridRagPipeline(Retriever retriever) {
        this.retriever = retriever;
    }

    @Override public String name() { return NAME; }
    @Override public List<SearchResult> search(Retriever.RetrievalRequest request) { return retriever.search(request); }
    @Override public List<MaterialChunk> retrieveEvidence(Retriever.RetrievalRequest request, int limit) {
        return retriever.retrieveEvidence(request, limit);
    }
}
