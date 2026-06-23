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
    private static final double MIN_SEMANTIC_SCORE = 0.35;
    private static final double MIN_RETRIEVAL_SCORE = 0.15;
    private static final int EVIDENCE_OVERFETCH_MULTIPLIER = 4;

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
            .map(chunk -> scoredChunk(chunk, normalizedQuery, queryEmbedding))
            .filter(scored -> scored.score() >= MIN_RETRIEVAL_SCORE)
            .forEach(scored -> store.findMaterial(scored.chunk().materialId()).ifPresent(material -> results.add(new SearchResult(
                scored.chunk().id(),
                SearchResultType.MATERIAL_CHUNK,
                material.title(),
                truncate(scored.chunk().content()),
                scored.score(),
                material.knowledgeBaseId(),
                List.of(),
                List.of(scored.chunk().sourceRef())
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
            .map(chunk -> scoredChunk(chunk, normalizedQuery, queryEmbedding))
            .filter(scored -> scored.score() >= MIN_RETRIEVAL_SCORE)
            .sorted(Comparator.comparing(ScoredChunk::score).reversed())
            .limit((long) limit * EVIDENCE_OVERFETCH_MULTIPLIER)
            .collect(() -> new DiversifiedEvidence(limit, request.materialId()), DiversifiedEvidence::add, DiversifiedEvidence::addAll)
            .chunks()
            .stream()
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

    private ScoredChunk scoredChunk(MaterialChunk chunk, String normalizedQuery, List<Double> queryEmbedding) {
        return new ScoredChunk(chunk, combinedScore(chunk.content(), normalizedQuery, queryEmbedding, chunk));
    }

    private boolean containsAnyKeyword(String content, String normalizedQuery) {
        for (var keyword : keywords(normalizedQuery)) {
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
        var keywordScore = keywordScore(content, normalizedQuery);
        var semanticScore = semanticScore(queryEmbedding, chunk);
        if (keywordScore == 0.0 && semanticScore == 0.0) {
            return 0.0;
        }
        var coverageScore = coverageScore(content, normalizedQuery);
        var compactnessScore = compactnessScore(content);
        return clamp((semanticScore * 0.55) + (keywordScore * 0.30) + (coverageScore * 0.10) + (compactnessScore * 0.05));
    }

    private double semanticScore(List<Double> queryEmbedding, MaterialChunk chunk) {
        if (chunk.embeddingModel() == null) {
            return 0.0;
        }
        var score = cosine(queryEmbedding, chunk.embedding());
        return score < MIN_SEMANTIC_SCORE ? 0.0 : score;
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
        for (var keyword : keywords(normalizedQuery)) {
            if (!keyword.isBlank()) {
                total++;
                if (normalizedContent.contains(keyword)) {
                    hits++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hits / (double) total;
    }

    private double coverageScore(String content, String normalizedQuery) {
        if (content == null || normalizedQuery == null || normalizedQuery.isBlank()) {
            return 0.0;
        }
        var normalizedContent = normalize(content);
        var hits = 0;
        var total = 0;
        for (var keyword : keywords(normalizedQuery)) {
            if (!keyword.isBlank()) {
                total++;
                if (normalizedContent.contains(keyword)) {
                    hits++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hits / (double) total;
    }

    private double compactnessScore(String content) {
        if (content == null || content.isBlank()) {
            return 0.0;
        }
        var length = content.length();
        if (length <= 600) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - ((double) (length - 600) / 1200.0));
    }

    private List<String> keywords(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return List.of();
        }
        return List.of(normalizedQuery.split("[^\\p{L}\\p{N}]+")).stream()
            .filter(keyword -> !keyword.isBlank())
            .toList();
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

    private record ScoredChunk(MaterialChunk chunk, double score) {
    }

    private static class DiversifiedEvidence {
        private final int limit;
        private final boolean strictMaterialScope;
        private final List<ScoredChunk> selected = new ArrayList<>();
        private final List<ScoredChunk> deferred = new ArrayList<>();

        private DiversifiedEvidence(int limit, String materialId) {
            this.limit = limit;
            this.strictMaterialScope = materialId != null && !materialId.isBlank();
        }

        private void add(ScoredChunk candidate) {
            if (selected.size() >= limit) {
                return;
            }
            if (strictMaterialScope || selected.stream().noneMatch(existing -> sameMaterial(existing, candidate))) {
                selected.add(candidate);
                return;
            }
            deferred.add(candidate);
        }

        private void addAll(DiversifiedEvidence other) {
            other.selected.forEach(this::add);
            other.deferred.forEach(this::add);
        }

        private List<MaterialChunk> chunks() {
            if (selected.size() < limit) {
                for (var candidate : deferred) {
                    if (selected.size() >= limit) {
                        break;
                    }
                    selected.add(candidate);
                }
            }
            return selected.stream().map(ScoredChunk::chunk).toList();
        }

        private boolean sameMaterial(ScoredChunk left, ScoredChunk right) {
            return left.chunk().materialId().equals(right.chunk().materialId());
        }
    }
}
