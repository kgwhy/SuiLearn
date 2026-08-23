package com.suilearn.api.rag.pipeline;

import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.retrieval.Retriever;
import java.util.List;

public interface RagPipeline {
    String name();

    List<SearchResult> search(Retriever.RetrievalRequest request);

    List<MaterialChunk> retrieveEvidence(Retriever.RetrievalRequest request, int limit);
}
