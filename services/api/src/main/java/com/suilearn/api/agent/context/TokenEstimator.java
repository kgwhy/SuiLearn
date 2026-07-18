package com.suilearn.api.agent.context;

@FunctionalInterface
public interface TokenEstimator {
    int estimate(String content);

    static TokenEstimator conservativeCharacters() {
        return content -> Math.max(1, Math.ceilDiv(content.codePointCount(0, content.length()), 4));
    }
}
