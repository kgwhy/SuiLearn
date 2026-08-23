package com.suilearn.api.rag.parsing;

public interface ParseEngine {
    String name();
    boolean supports(String mediaType);
    ParsedDocument parse(String mediaType, byte[] content);
}
