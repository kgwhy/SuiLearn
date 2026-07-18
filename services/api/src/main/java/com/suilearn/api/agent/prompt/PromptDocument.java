package com.suilearn.api.agent.prompt;

public record PromptDocument(String name, String version, String sha256, String content) {
    public PromptDocument {
        if (name == null || version == null || sha256 == null || content == null || content.isBlank()) {
            throw new IllegalArgumentException("prompt metadata and content are required");
        }
        if (!sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256 must be lowercase hexadecimal");
        }
    }
}
