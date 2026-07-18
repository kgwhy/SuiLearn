package com.suilearn.api.agent.memory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class SessionMemoryKeyFactory {
    private final String controlledPrefix;

    public SessionMemoryKeyFactory(String controlledPrefix) {
        if (controlledPrefix == null || !controlledPrefix.matches("[a-z0-9:-]+")) {
            throw new IllegalArgumentException("session key prefix must be controlled lowercase text");
        }
        this.controlledPrefix = controlledPrefix;
    }

    public String key(String learnerId, String sessionId) {
        return learnerPrefix(learnerId) + hash(requireText(sessionId, "sessionId"));
    }

    public String learnerPrefix(String learnerId) {
        return controlledPrefix + ":" + hash(requireText(learnerId, "learnerId")) + ":";
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
