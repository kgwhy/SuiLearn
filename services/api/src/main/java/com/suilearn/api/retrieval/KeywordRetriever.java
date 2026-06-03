package com.suilearn.api.retrieval;

import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KeywordRetriever implements Retriever {
    private final EmbeddingProvider embeddingProvider;
    private final SuiLearnV2Store store;

    public KeywordRetriever(EmbeddingProvider embeddingProvider, SuiLearnV2Store store) {
        this.embeddingProvider = embeddingProvider;
        this.store = store;
    }

    @Override
    public List<SearchResult> search(RetrievalRequest request) {
        var normalizedQuery = normalize(request.query());
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        embeddingProvider.embed(request.query());
        var results = new ArrayList<SearchResult>();
        store.listKnowledgePoints().stream()
            .filter(point -> matchesScope(point.knowledgeBaseId(), request.knowledgeBaseId()))
            .filter(point -> !isMaterialDeleted(point.sourceMaterialId()))
            .filter(point -> request.materialId() == null || request.materialId().equals(point.sourceMaterialId())
                || referencesMaterial(point.sourceRefs(), request.materialId()))
            .filter(point -> contains(point.name(), normalizedQuery) || contains(point.description(), normalizedQuery))
            .forEach(point -> results.add(new SearchResult(
                point.id(),
                SearchResultType.KNOWLEDGE_POINT,
                point.name(),
                point.description(),
                1.0,
                point.knowledgeBaseId(),
                List.of(point.id()),
                point.sourceRefs()
            )));
        store.listChunks().stream()
            .filter(chunk -> chunk.embeddingStatus() == EmbeddingStatus.READY)
            .filter(chunk -> {
                var material = store.findMaterial(chunk.materialId()).orElse(null);
                return material != null
                    && material.status() != MaterialStatus.DELETED
                    && matchesScope(material.knowledgeBaseId(), request.knowledgeBaseId())
                    && (request.materialId() == null || request.materialId().equals(material.id()));
            })
            .filter(chunk -> contains(chunk.content(), normalizedQuery))
            .forEach(chunk -> store.findMaterial(chunk.materialId()).ifPresent(material -> results.add(new SearchResult(
                chunk.id(),
                SearchResultType.MATERIAL_CHUNK,
                material.title(),
                truncate(chunk.content()),
                1.0,
                material.knowledgeBaseId(),
                List.of(),
                List.of(chunk.sourceRef())
            ))));
        store.listQuestions().stream()
            .filter(question -> matchesScope(question.knowledgeBaseId(), request.knowledgeBaseId()))
            .filter(question -> request.materialId() == null || referencesMaterial(question.sourceRefs(), request.materialId()))
            .filter(question -> contains(question.stem(), normalizedQuery))
            .forEach(question -> results.add(new SearchResult(
                question.id(),
                SearchResultType.QUESTION,
                question.stem(),
                question.stem(),
                1.0,
                question.knowledgeBaseId(),
                question.knowledgePointIds(),
                question.sourceRefs()
            )));
        store.listGeneratedContents().stream()
            .filter(content -> matchesScope(content.knowledgeBaseId(), request.knowledgeBaseId()))
            .filter(content -> request.materialId() == null || referencesMaterial(content.sourceRefs(), request.materialId()))
            .filter(content -> content.status() == GeneratedContentStatus.SAVED)
            .filter(content -> contains(content.stem(), normalizedQuery) || contains(content.explanation(), normalizedQuery))
            .forEach(content -> results.add(new SearchResult(
                content.id(),
                SearchResultType.GENERATED_CONTENT,
                content.stem(),
                content.explanation(),
                1.0,
                content.knowledgeBaseId(),
                List.of(),
                content.sourceRefs()
            )));
        return results;
    }

    @Override
    public List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) {
        var normalizedQuery = normalize(request.query());
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        embeddingProvider.embed(request.query());
        return store.listChunks().stream()
            .filter(chunk -> chunk.embeddingStatus() == EmbeddingStatus.READY)
            .filter(chunk -> request.materialId() == null || chunk.materialId().equals(request.materialId()))
            .filter(chunk -> {
                var material = store.findMaterial(chunk.materialId()).orElse(null);
                return material != null
                    && material.status() != MaterialStatus.DELETED
                    && matchesScope(material.knowledgeBaseId(), request.knowledgeBaseId());
            })
            .filter(chunk -> containsAnyKeyword(chunk.content(), normalizedQuery))
            .limit(limit)
            .toList();
    }

    private boolean referencesMaterial(List<SourceRef> refs, String materialId) {
        return refs != null && refs.stream().anyMatch(ref -> referencesMaterial(ref, materialId));
    }

    private boolean referencesMaterial(SourceRef ref, String materialId) {
        return ref != null
            && (materialId.equals(ref.materialId())
                || (ref.type() == SourceType.MATERIAL && materialId.equals(ref.id())));
    }

    private boolean isMaterialDeleted(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return false;
        }
        return store.findMaterial(materialId)
            .map(material -> material.status() == MaterialStatus.DELETED)
            .orElse(false);
    }

    private boolean containsAnyKeyword(String content, String normalizedQuery) {
        for (var keyword : normalizedQuery.split("\\s+")) {
            if (!keyword.isBlank() && contains(content, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && normalize(value).contains(normalizedQuery);
    }

    private boolean matchesScope(String valueKnowledgeBaseId, String requestedKnowledgeBaseId) {
        return requestedKnowledgeBaseId == null || requestedKnowledgeBaseId.isBlank() || valueKnowledgeBaseId.equals(requestedKnowledgeBaseId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }
}
