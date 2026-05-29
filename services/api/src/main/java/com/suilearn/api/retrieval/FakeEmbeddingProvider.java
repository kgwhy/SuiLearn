package com.suilearn.api.retrieval;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FakeEmbeddingProvider implements EmbeddingProvider {
    @Override
    public Embedding embed(String input) {
        var normalized = input == null ? "" : input.toLowerCase().trim();
        var length = (double) normalized.length();
        var tokenCount = normalized.isBlank() ? 0.0 : (double) normalized.split("\\s+").length;
        var hashBucket = (double) Math.floorMod(normalized.hashCode(), 1000) / 1000.0;
        return new Embedding(List.of(length, tokenCount, hashBucket));
    }
}
