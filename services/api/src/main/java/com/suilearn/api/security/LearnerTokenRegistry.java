package com.suilearn.api.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory bearer token registry bound to learner ids. Tokens are supplied as a
 * JSON array in configuration and are never written to logs or responses.
 */
public final class LearnerTokenRegistry {
    private static final int MIN_TOKEN_LENGTH = 8;

    private final Map<String, LearnerPrincipal> principalsByToken;

    public LearnerTokenRegistry(List<TokenBinding> bindings) {
        var copy = new LinkedHashMap<String, LearnerPrincipal>();
        for (TokenBinding binding : bindings == null ? List.<TokenBinding>of() : bindings) {
            String token = binding.token() == null ? "" : binding.token().strip();
            if (token.length() < MIN_TOKEN_LENGTH) {
                throw new IllegalArgumentException("learner auth token must be at least " + MIN_TOKEN_LENGTH + " characters");
            }
            var principal = new LearnerPrincipal(binding.learnerId());
            if (copy.putIfAbsent(token, principal) != null) {
                throw new IllegalArgumentException("duplicate learner auth token");
            }
        }
        this.principalsByToken = Map.copyOf(copy);
    }

    public static LearnerTokenRegistry fromJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return new LearnerTokenRegistry(List.of());
        }
        try {
            return new LearnerTokenRegistry(objectMapper.readValue(json, new TypeReference<List<TokenBinding>>() { }));
        } catch (Exception exception) {
            throw new IllegalArgumentException("suilearn.auth.tokens must be a JSON array of {token, learnerId}", exception);
        }
    }

    public Optional<LearnerPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String candidate = token.strip();
        for (var entry : principalsByToken.entrySet()) {
            if (constantTimeEquals(entry.getKey(), candidate)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public int size() {
        return principalsByToken.size();
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
            left.getBytes(StandardCharsets.UTF_8),
            right.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record TokenBinding(String token, String learnerId) {
    }
}
