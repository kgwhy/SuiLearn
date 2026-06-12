package com.suilearn.api.search.application;

import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.retrieval.Retriever;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
    private final KnowledgeBaseStore knowledgeBases;
    private final MaterialStore materials;
    private final Retriever retriever;

    public SearchService(KnowledgeBaseStore knowledgeBases, MaterialStore materials, Retriever retriever) {
        this.knowledgeBases = knowledgeBases;
        this.materials = materials;
        this.retriever = retriever;
    }

    public List<SearchResult> search(String query, String knowledgeBaseId, String materialId) {
        var scope = requireSearchScope(knowledgeBaseId, materialId);
        return retriever.search(new Retriever.RetrievalRequest(query, scope.knowledgeBaseId(), scope.materialId()));
    }

    private SearchScope requireSearchScope(String knowledgeBaseId, String materialId) {
        if (isBlank(knowledgeBaseId) && isBlank(materialId)) {
            throw new IllegalArgumentException("At least one scope is required: knowledgeBaseId or materialId");
        }
        if (!isBlank(materialId)) {
            var material = requireMaterial(materialId);
            if (!isBlank(knowledgeBaseId) && !material.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("Material is outside knowledge base: " + materialId);
            }
            return new SearchScope(material.knowledgeBaseId(), material.id());
        }
        requireKnowledgeBase(knowledgeBaseId);
        return new SearchScope(knowledgeBaseId, null);
    }

    private void requireKnowledgeBase(String knowledgeBaseId) {
        knowledgeBases.find(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private LearningMaterial requireMaterial(String materialId) {
        return materials.find(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SearchScope(String knowledgeBaseId, String materialId) {
    }
}
