package com.suilearn.api.ai.application;

import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import java.util.List;

public interface RetrievalPort {
    List<SearchResult> search(RetrievalRequest request);

    List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit);

    record RetrievalRequest(String query, String knowledgeBaseId, String materialId) {
    }
}
