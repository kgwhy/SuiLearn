package com.suilearn.api.rag.index;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record EmbeddingSignature(String binding, String model, int dimensions,
                                 String baseUrl, String apiVersion) {
    public EmbeddingSignature {
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
        if (dimensions < 1) throw new IllegalArgumentException("dimensions must be positive");
        binding = binding == null || binding.isBlank() ? "default" : binding;
        baseUrl = baseUrl == null ? "" : baseUrl;
        apiVersion = apiVersion == null ? "" : apiVersion;
    }

    public String hash() {
        String canonical = binding + "\u0000" + model + "\u0000" + dimensions + "\u0000" + baseUrl + "\u0000" + apiVersion;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
