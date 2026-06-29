package com.suilearn.api.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.suilearn.api.model.MaterialChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorpusStatsTest {
    private final TextSearchTokenizer tokenizer = new TextSearchTokenizer();

    @Test
    void bm25MatchesHandComputedTextbookFormula() {
        var a = chunk("a", "alpha beta");
        var b = chunk("b", "alpha gamma");
        var stats = CorpusStats.of(List.of(a, b), tokenizer);
        var terms = tokenizer.tokens("alpha beta");

        // N=2, avgdl=2, df(alpha)=2, df(beta)=1；tokens() 去重导致 tf 恒为 1（二元 BM25）。
        // idf = log(1 + (N - df + 0.5) / (df + 0.5))
        var idfAlpha = Math.log(1.0 + (2 - 2 + 0.5) / (2 + 0.5));
        var idfBeta = Math.log(1.0 + (2 - 1 + 0.5) / (1 + 0.5));
        // denominator = 1 + 1.5*(1-0.75+0.75*docLen/avgdl) = 1 + 1.5*1 = 2.5；
        // (f*(k1+1))/denom = (1*2.5)/2.5 = 1，故每个命中 term 的贡献恰为其 idf。
        assertThat(stats.bm25("a", terms)).isCloseTo(idfAlpha + idfBeta, within(1e-9));
        assertThat(stats.bm25("b", terms)).isCloseTo(idfAlpha, within(1e-9));
    }

    @Test
    void unmatchedTermsContributeZero() {
        var stats = CorpusStats.of(List.of(chunk("a", "alpha beta"), chunk("b", "alpha gamma")), tokenizer);
        // chunk b 不含 beta，对 b 用 [beta] 打分应为 0。
        assertThat(stats.bm25("b", tokenizer.tokens("beta"))).isZero();
    }

    @Test
    void emptyCorpusAndUnknownChunkYieldZero() {
        var empty = CorpusStats.of(List.of(), tokenizer);
        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.bm25("x", tokenizer.tokens("alpha"))).isZero();

        var stats = CorpusStats.of(List.of(chunk("a", "alpha")), tokenizer);
        assertThat(stats.bm25("missing", tokenizer.tokens("alpha"))).isZero();
        assertThat(stats.bm25("a", List.of())).isZero();
    }

    @Test
    void chineseBigramTokensAreCounted() {
        var stats = CorpusStats.of(List.of(chunk("a", "机器学习")), tokenizer);
        // 机器学习 -> 机,器,机器,学,器学,习,学习；查询「机器」-> 机,器,机器 均命中，分数为正。
        assertThat(stats.bm25("a", tokenizer.tokens("机器"))).isGreaterThan(0.0);
    }

    private MaterialChunk chunk(String id, String content) {
        return new MaterialChunk(id, "mat", content, 0, null);
    }
}
