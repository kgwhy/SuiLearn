package com.suilearn.api.agent.prompt;

public record PromptKey(String name, String version) {
    public PromptKey {
        if (name == null || !name.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException("invalid prompt name");
        }
        if (version == null || !version.matches("v[1-9][0-9]*")) {
            throw new IllegalArgumentException("invalid prompt version");
        }
    }
}
