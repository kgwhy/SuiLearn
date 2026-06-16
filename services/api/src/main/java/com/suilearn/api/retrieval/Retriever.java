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
        String materialId,
        int limit
    ) {
        public static final int DEFAULT_LIMIT = 10;
        public static final int MAX_LIMIT = 50;

        public RetrievalRequest(String query, String knowledgeBaseId, String materialId) {
            this(query, knowledgeBaseId, materialId, DEFAULT_LIMIT);
        }

        public RetrievalRequest {
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new IllegalArgumentException("Search limit must be between 1 and " + MAX_LIMIT);
            }
        }
    }
}
