package com.suilearn.api.retrieval;

import com.suilearn.api.model.MaterialChunk;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单次查询的语料统计，按候选集预计算一次：每个 chunk 的词频与长度、各 term 的
 * document frequency、平均文档长度。消除了原实现中「对每个候选都遍历整个语料重新
 * 分词」的 O(N²) 行为。统计口径与 BM25 打分使用同一 {@link TextSearchTokenizer}。
 *
 * <p>BM25 公式（{@link #bm25}）与逐候选实现等价：k1=1.5、b=0.75，IDF 为
 * {@code log(1 + (N - df + 0.5) / (df + 0.5))}。提为包级顶层类以便对拍单测。
 */
final class CorpusStats {
    private static final double K1 = 1.5;
    private static final double B = 0.75;

    private final Map<String, Map<String, Integer>> termFrequenciesByChunk;
    private final Map<String, Integer> documentLengthByChunk;
    private final Map<String, Integer> documentFrequencyByTerm;
    private final double averageDocumentLength;
    private final int size;

    private CorpusStats(
        Map<String, Map<String, Integer>> termFrequenciesByChunk,
        Map<String, Integer> documentLengthByChunk,
        Map<String, Integer> documentFrequencyByTerm,
        double averageDocumentLength,
        int size
    ) {
        this.termFrequenciesByChunk = termFrequenciesByChunk;
        this.documentLengthByChunk = documentLengthByChunk;
        this.documentFrequencyByTerm = documentFrequencyByTerm;
        this.averageDocumentLength = averageDocumentLength;
        this.size = size;
    }

    static CorpusStats of(List<MaterialChunk> corpus, TextSearchTokenizer tokenizer) {
        var termFrequenciesByChunk = new HashMap<String, Map<String, Integer>>();
        var documentLengthByChunk = new HashMap<String, Integer>();
        var documentFrequencyByTerm = new HashMap<String, Integer>();
        var totalLength = 0L;
        for (var chunk : corpus) {
            var frequencies = new HashMap<String, Integer>();
            for (var term : tokenizer.tokens(chunk.content())) {
                frequencies.merge(term, 1, Integer::sum);
            }
            termFrequenciesByChunk.put(chunk.id(), frequencies);
            var length = frequencies.values().stream().mapToInt(Integer::intValue).sum();
            documentLengthByChunk.put(chunk.id(), length);
            totalLength += length;
            for (var term : frequencies.keySet()) {
                documentFrequencyByTerm.merge(term, 1, Integer::sum);
            }
        }
        var averageDocumentLength = corpus.isEmpty() ? 1.0 : (double) totalLength / corpus.size();
        return new CorpusStats(
            termFrequenciesByChunk,
            documentLengthByChunk,
            documentFrequencyByTerm,
            averageDocumentLength,
            corpus.size()
        );
    }

    boolean isEmpty() {
        return size == 0;
    }

    int size() {
        return size;
    }

    double averageDocumentLength() {
        return averageDocumentLength;
    }

    Map<String, Integer> termFrequencies(String chunkId) {
        return termFrequenciesByChunk.getOrDefault(chunkId, Map.of());
    }

    int documentLength(String chunkId) {
        return documentLengthByChunk.getOrDefault(chunkId, 0);
    }

    int documentFrequency(String term) {
        return documentFrequencyByTerm.getOrDefault(term, 0);
    }

    /** 对给定 chunk 计算 BM25 分数；预计算统计直接查表，不重新分词。 */
    double bm25(String chunkId, List<String> terms) {
        if (terms.isEmpty() || isEmpty()) {
            return 0.0;
        }
        var frequencies = termFrequencies(chunkId);
        if (frequencies.isEmpty()) {
            return 0.0;
        }
        var documentLength = documentLength(chunkId);
        var score = 0.0;
        for (var term : terms) {
            var frequency = frequencies.getOrDefault(term, 0);
            if (frequency == 0) {
                continue;
            }
            var df = documentFrequency(term);
            var idf = Math.log(1.0 + ((size - df + 0.5) / (df + 0.5)));
            var denominator = frequency + K1 * (1.0 - B + B * documentLength / Math.max(1.0, averageDocumentLength));
            score += idf * (frequency * (K1 + 1.0)) / denominator;
        }
        return score;
    }
}
