package com.suilearn.api.retrieval;

import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KeywordRetriever implements Retriever {
    private static final double MIN_SEMANTIC_SCORE = 0.35;
    private static final double MIN_RETRIEVAL_SCORE = 0.15;
    private static final int EVIDENCE_OVERFETCH_MULTIPLIER = 4;
    // 索引仅做候选预筛选，BM25 再排序；text-only 路径有界候选上限，优先保性能。
    private static final int CANDIDATE_INDEX_LIMIT = 50;

    private final EmbeddingProvider embeddingProvider;
    private final SuiLearnV2Store store;
    private final TextSearchTokenizer tokenizer;

    public KeywordRetriever(
        EmbeddingProvider embeddingProvider,
        SuiLearnV2Store store,
        TextSearchTokenizer tokenizer
    ) {
        this.embeddingProvider = embeddingProvider;
        this.store = store;
        this.tokenizer = tokenizer;
    }

    @Override
    public List<SearchResult> search(RetrievalRequest request) {
        var normalizedQuery = normalize(request.query());
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        var queryEmbedding = queryEmbedding(request.query());
        var scopedChunks = retrievableChunks(request);
        var candidateChunks = candidateChunks(request, scopedChunks);
        var corpusStats = CorpusStats.of(scopedChunks, tokenizer);
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
        candidateChunks.stream()
            .map(chunk -> scoredChunk(chunk, normalizedQuery, queryEmbedding, corpusStats))
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
        var queryEmbedding = queryEmbedding(request.query());
        var scopedChunks = retrievableChunks(request);
        var candidateChunks = candidateChunks(request, scopedChunks);
        var corpusStats = CorpusStats.of(scopedChunks, tokenizer);
        var scored = candidateChunks.stream()
            .map(chunk -> scoredChunk(chunk, normalizedQuery, queryEmbedding, corpusStats))
            .filter(candidate -> candidate.score() >= MIN_RETRIEVAL_SCORE)
            .sorted(Comparator.comparing(ScoredChunk::score).reversed())
            .limit((long) limit * EVIDENCE_OVERFETCH_MULTIPLIER)
            .toList();
        return expandEvidence(scored, scopedChunks, limit, request.materialId());
    }

    private List<MaterialChunk> retrievableChunks(RetrievalRequest request) {
        // scope/删除/embedding_status 过滤已在 SQL 完成，避免全表 findAll() 与逐 chunk 的 N+1。
        return store.listChunksByScope(request.knowledgeBaseId(), request.materialId());
    }

    private List<MaterialChunk> candidateChunks(RetrievalRequest request, List<MaterialChunk> scopedChunks) {
        // 语义模式下，索引（仅基于关键词）无法预过滤「语义相关但无关键词重叠」的 chunk，
        // 否则会丢失语义召回；此时对已加载的 scope 候选直接打分。索引收窄只用于 text-only
        // 主路径——这也是本变更要消除全表扫描的目标场景。
        if (embeddingProvider.supportsEmbeddings()) {
            return scopedChunks;
        }
        var indexed = store.searchChunksText(
            request.query(),
            request.knowledgeBaseId(),
            request.materialId(),
            Math.max(request.limit(), CANDIDATE_INDEX_LIMIT)
        );
        if (indexed == null || indexed.isEmpty()) {
            return scopedChunks;
        }
        var indexedIds = indexed.stream().map(MaterialChunk::id).collect(java.util.stream.Collectors.toSet());
        return scopedChunks.stream()
            .filter(chunk -> indexedIds.contains(chunk.id()))
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

    private ScoredChunk scoredChunk(
        MaterialChunk chunk,
        String normalizedQuery,
        List<Double> queryEmbedding,
        CorpusStats stats
    ) {
        return new ScoredChunk(chunk, combinedScore(chunk.content(), normalizedQuery, queryEmbedding, chunk, stats));
    }

    private List<Double> queryEmbedding(String query) {
        if (!embeddingProvider.supportsEmbeddings()) {
            return List.of();
        }
        try {
            return embeddingProvider.embed(query).values();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && normalize(value).contains(normalizedQuery);
    }

    private boolean matchesScope(String valueKnowledgeBaseId, String requestedKnowledgeBaseId) {
        return requestedKnowledgeBaseId == null || requestedKnowledgeBaseId.isBlank() || valueKnowledgeBaseId.equals(requestedKnowledgeBaseId);
    }

    private double combinedScore(
        String content,
        String normalizedQuery,
        List<Double> queryEmbedding,
        MaterialChunk chunk,
        CorpusStats stats
    ) {
        var terms = keywords(normalizedQuery);
        var keywordScore = textOnlyScore(content, normalizedQuery, terms, chunk, stats);
        var semanticScore = semanticScore(queryEmbedding, chunk);
        if (keywordScore == 0.0 && semanticScore == 0.0) {
            return 0.0;
        }
        var compactnessScore = compactnessScore(content);
        if (semanticScore > 0.0) {
            return clamp((semanticScore * 0.55) + (keywordScore * 0.40) + (compactnessScore * 0.05));
        }
        return clamp((keywordScore * 0.95) + (compactnessScore * 0.05));
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

    private double textOnlyScore(
        String content,
        String normalizedQuery,
        List<String> terms,
        MaterialChunk chunk,
        CorpusStats stats
    ) {
        if (content == null || normalizedQuery == null || normalizedQuery.isBlank()) {
            return 0.0;
        }
        var normalizedContent = normalize(content);
        var phraseScore = normalizedContent.contains(normalizedQuery) ? 1.0 : 0.0;
        var bm25 = bm25Score(chunk, terms, stats);
        var normalizedBm25 = bm25 <= 0.0 ? 0.0 : bm25 / (bm25 + 3.0);
        var coverageScore = coverageScore(content, normalizedQuery);
        return clamp((normalizedBm25 * 0.60) + (phraseScore * 0.25) + (coverageScore * 0.15));
    }

    /**
     * BM25 打分使用每次查询预计算一次的 {@link CorpusStats}，而不是对每个候选重新遍历
     * 整个语料分词。打分公式与逐候选实现等价（k1=1.5、b=0.75、相同 IDF）。
     */
    private double bm25Score(MaterialChunk chunk, List<String> terms, CorpusStats stats) {
        return stats.bm25(chunk.id(), terms);
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
        return tokenizer.tokens(normalizedQuery);
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

    private List<MaterialChunk> expandEvidence(
        List<ScoredChunk> scored,
        List<MaterialChunk> scopedChunks,
        int limit,
        String materialId
    ) {
        var bases = scored.stream()
            .collect(() -> new DiversifiedEvidence(limit, materialId), DiversifiedEvidence::add, DiversifiedEvidence::addAll)
            .chunks();
        var byNeighborKey = new HashMap<String, MaterialChunk>();
        for (var chunk : scopedChunks) {
            byNeighborKey.put(neighborKey(chunk.materialId(), chunk.ordinal()), chunk);
        }
        var selected = new LinkedHashMap<String, MaterialChunk>();
        for (var base : bases) {
            addEvidence(selected, base, limit);
            addEvidence(selected, byNeighborKey.get(neighborKey(base.materialId(), base.ordinal() - 1)), limit);
            addEvidence(selected, byNeighborKey.get(neighborKey(base.materialId(), base.ordinal() + 1)), limit);
            if (selected.size() >= limit) {
                break;
            }
        }
        return selected.values().stream().toList();
    }

    private void addEvidence(LinkedHashMap<String, MaterialChunk> selected, MaterialChunk chunk, int limit) {
        if (chunk != null && selected.size() < limit) {
            selected.putIfAbsent(chunk.id(), chunk);
        }
    }

    private String neighborKey(String materialId, int ordinal) {
        return materialId + "#" + ordinal;
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
