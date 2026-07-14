package com.suilearn.api.config;

final class AdapterRetryConfigurationResolver {
    static final String LEGACY_MAPPED = "SUILEARN_RETRY_CONFIG_LEGACY_MAPPED";
    static final String CONFLICT = "SUILEARN_RETRY_CONFIG_CONFLICT";

    private AdapterRetryConfigurationResolver() {
    }

    static ResolvedAdapterRetryConfiguration resolve(String canonicalValue, String legacyValue) {
        var canonical = explicitValue(canonicalValue);
        var legacy = explicitValue(legacyValue);
        if (canonical != null && legacy != null) {
            throw new IllegalStateException(CONFLICT + ": SUILEARN_ADAPTER_MAX_RETRIES and SUILEARN_AI_MAX_RETRIES cannot both be set");
        }
        if (canonical != null) {
            var retries = parseInteger(canonical, "SUILEARN_ADAPTER_MAX_RETRIES");
            if (retries < 0 || retries > 1) {
                throw new IllegalStateException("SUILEARN_ADAPTER_MAX_RETRIES must be an integer from 0 to 1");
            }
            return new ResolvedAdapterRetryConfiguration(retries, null);
        }
        if (legacy != null) {
            var retries = parseInteger(legacy, "SUILEARN_AI_MAX_RETRIES");
            if (retries < 0) {
                throw new IllegalStateException("SUILEARN_AI_MAX_RETRIES must be a non-negative integer");
            }
            return new ResolvedAdapterRetryConfiguration(retries == 0 ? 0 : 1, LEGACY_MAPPED);
        }
        return new ResolvedAdapterRetryConfiguration(0, null);
    }

    private static String explicitValue(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int parseInteger(String value, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(key + " must be an integer", exception);
        }
    }

    record ResolvedAdapterRetryConfiguration(int maxRetries, String diagnosticCode) {
    }
}
