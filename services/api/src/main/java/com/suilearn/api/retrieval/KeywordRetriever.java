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
import java.util.Comparator;
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
        var queryEmbedding = embeddingProvider.embed(request.query()).values();
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
                keywordScore(point.name() + " " + point.description(), normalizedQuery),
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
            .filter(chunk -> containsAnyKeyword(chunk.content(), normalizedQuery) || semanticScore(queryEmbedding, chunk) > 0.0)
            .forEach(chunk -> store.findMaterial(chunk.materialId()).ifPresent(material -> results.add(new SearchResult(
                chunk.id(),
                SearchResultType.MATERIAL_CHUNK,
                material.title(),
                truncate(chunk.content()),
                combinedScore(chunk.content(), normalizedQuery, queryEmbedding, chunk),
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
                keywordScore(question.stem(), normalizedQuery),
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
                keywordScore(content.stem() + " " + content.explanation(), normalizedQuery),
                content.knowledgeBaseId(),
                List.of(),
                content.sourceRefs()
            )));
        return results.stream()
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .limit(request.limit())
            .toList();
    }

    @Override
    public List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) {
        var normalizedQuery = normalize(request.query());
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        var queryEmbedding = embeddingProvider.embed(request.query()).values();
        return store.listChunks().stream()
            .filter(chunk -> chunk.embeddingStatus() == EmbeddingStatus.READY)
            .filter(chunk -> request.materialId() == null || chunk.materialId().equals(request.materialId()))
            .filter(chunk -> {
                var material = store.findMaterial(chunk.materialId()).orElse(null);
                return material != null
                    && material.status() != MaterialStatus.DELETED
                    && matchesScope(material.knowledgeBaseId(), request.knowledgeBaseId());
            })
            .filter(chunk -> containsAnyKeyword(chunk.content(), normalizedQuery) || semanticScore(queryEmbedding, chunk) > 0.0)
            .sorted(Comparator.comparing((MaterialChunk chunk) -> combinedScore(
                chunk.content(),
                normalizedQuery,
                queryEmbedding,
                chunk
            )).reversed())
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

    private double combinedScore(String content, String normalizedQuery, List<Double> queryEmbedding, MaterialChunk chunk) {
        return clamp(Math.max(keywordScore(content, normalizedQuery), semanticScore(queryEmbedding, chunk)));
    }

    private double semanticScore(List<Double> queryEmbedding, MaterialChunk chunk) {
        if (chunk.embeddingModel() == null || "fake-embedding-v1".equals(chunk.embeddingModel())) {
            return 0.0;
        }
        var score = cosine(queryEmbedding, chunk.embedding());
        return score < 0.35 ? 0.0 : score;
    }

    private double keywordScore(String content, String normalizedQuery) {
        if (content == null || normalizedQuery == null || normalizedQuery.isBlank()) {
            return 0.0;
        }
        var normalizedContent = normalize(content);
        if (normalizedContent.contains(normalizedQuery)) {
            return 1.0;
        }
        var hits = 0;
        var total = 0;
        for (var keyword : normalizedQuery.split("\\s+")) {
            if (!keyword.isBlank()) {
                total++;
                if (normalizedContent.contains(keyword)) {
                    hits++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hits / (double) total;
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        var size = Math.min(left.size(), right.size());
        var dot = 0.0;
        var leftNorm = 0.0;
        var rightNorm = 0.0;
        for (var index = 0; index < size; index++) {
            var leftValue = left.get(index);
            var rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double clamp(double score) {
        if (Double.isNaN(score) || score < 0.0) {
            return 0.0;
        }
        return Math.min(score, 1.0);
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
