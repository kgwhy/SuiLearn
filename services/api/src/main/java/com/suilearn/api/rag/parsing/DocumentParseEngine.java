package com.suilearn.api.rag.parsing;

import com.suilearn.api.material.document.DocumentParser;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class DocumentParseEngine implements ParseEngine {
    private final String name;
    private final String mediaType;
    private final String fileName;
    private final DocumentParser parser;

    public DocumentParseEngine(String name, String mediaType, String fileName, DocumentParser parser) {
        this.name = name;
        this.mediaType = mediaType;
        this.fileName = fileName;
        this.parser = parser;
    }

    @Override public String name() { return name; }
    @Override public boolean supports(String candidate) { return mediaType.equalsIgnoreCase(candidate); }
    @Override public ParsedDocument parse(String candidate, byte[] content) {
        if (!supports(candidate)) throw new IllegalArgumentException("unsupported media type: " + candidate);
        var result = parser.parse(content, fileName, mediaType);
        if ("REJECTED".equals(result.disposition()) || result.blocks().isEmpty()) {
            throw new IllegalArgumentException("document parser rejected content for " + mediaType);
        }
        String text = result.blocks().stream().map(DocumentParser.Block::content)
            .reduce("", (a, b) -> a.isBlank() ? b : a + "\n\n" + b);
        return new ParsedDocument(mediaType, text, Map.of("pageCount", result.pageCount()));
    }
}
