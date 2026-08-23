package com.suilearn.api.rag.parsing;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class TextParseEngine implements ParseEngine {
    @Override public String name() { return "text"; }
    @Override public boolean supports(String mediaType) {
        return mediaType != null && (mediaType.startsWith("text/")
            || mediaType.contains("markdown") || mediaType.equals("text/plain"));
    }
    @Override public ParsedDocument parse(String mediaType, byte[] content) {
        if (!supports(mediaType)) throw new IllegalArgumentException("unsupported media type: " + mediaType);
        return new ParsedDocument(mediaType, new String(content, StandardCharsets.UTF_8).strip(),
            Map.of("charset", "UTF-8"));
    }
}
