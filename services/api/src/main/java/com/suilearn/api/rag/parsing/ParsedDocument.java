package com.suilearn.api.rag.parsing;

import java.util.Map;

public record ParsedDocument(String mediaType, String text, Map<String, Object> metadata) {
    public ParsedDocument {
        if (mediaType == null || mediaType.isBlank() || text == null) {
            throw new IllegalArgumentException("mediaType and text are required");
        }
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
