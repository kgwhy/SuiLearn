package com.suilearn.api.retrieval;

import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import java.util.List;

public interface Retriever {
    List<SearchResult> search(RetrievalRequest request);

    List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit);

    record RetrievalRequest(
        String query,
        String knowledgeBaseId,
        String materialId
    ) {
    }
}
