package com.suilearn.api.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSearchTokenizerTest {
    private final TextSearchTokenizer tokenizer = new TextSearchTokenizer();

    @Test
    void splitsChineseIntoUnigramsAndAdjacentBigrams() {
        assertThat(tokenizer.tokens("机器学习"))
            .containsExactly("机", "器", "机器", "学", "器学", "习", "学习");
    }

    @Test
    void extractsLatinAndDigitTokensLowercased() {
        assertThat(tokenizer.tokens("HashMap 16 buckets"))
            .containsExactly("hashmap", "16", "buckets");
    }

    @Test
    void deduplicatesRepeatedTokens() {
        assertThat(tokenizer.tokens("test test")).containsExactly("test");
    }

    @Test
    void searchTextJoinsTokensWithSpaces() {
        assertThat(tokenizer.searchText("机器 abc")).isEqualTo("机 器 机器 abc");
    }

    @Test
    void tsqueryQuotesTokensAndJoinsWithOr() {
        assertThat(tokenizer.tsquery("机器 abc")).isEqualTo("'机' | '器' | '机器' | 'abc'");
    }

    @Test
    void blankInputProducesEmptyResults() {
        assertThat(tokenizer.tokens("   ")).isEmpty();
        assertThat(tokenizer.searchText("")).isEmpty();
        assertThat(tokenizer.tsquery(null)).isEmpty();
    }
}
