package com.suilearn.api.agent.memory;

public final class MemoryPromotionPolicy {
    private final double minimumConfidence;
    private final int minimumLength;
    private final int maximumLength;

    public MemoryPromotionPolicy(double minimumConfidence, int minimumLength, int maximumLength) {
        if (!Double.isFinite(minimumConfidence) || minimumConfidence < 0 || minimumConfidence > 1) {
            throw new IllegalArgumentException("minimumConfidence must be between 0 and 1");
        }
        if (minimumLength < 1 || maximumLength < minimumLength) {
            throw new IllegalArgumentException("invalid content length bounds");
        }
        this.minimumConfidence = minimumConfidence;
        this.minimumLength = minimumLength;
        this.maximumLength = maximumLength;
    }

    public PromotionDecision evaluate(String expectedLearnerId, MemoryCandidate candidate) {
        if (candidate == null || !hasText(expectedLearnerId) || !expectedLearnerId.equals(candidate.learnerId())) {
            return PromotionDecision.reject(PromotionRejection.LEARNER_MISMATCH);
        }
        if (candidate.memoryType() == null || !MemoryType.allowed().contains(candidate.memoryType())) {
            return PromotionDecision.reject(PromotionRejection.DISALLOWED_TYPE);
        }
        if (!Double.isFinite(candidate.confidence()) || candidate.confidence() < minimumConfidence) {
            return PromotionDecision.reject(PromotionRejection.LOW_CONFIDENCE);
        }
        int length = candidate.content() == null ? 0 : candidate.content().strip().length();
        if (length < minimumLength || length > maximumLength) {
            return PromotionDecision.reject(PromotionRejection.INVALID_LENGTH);
        }
        if (!hasText(candidate.sourceRunId()) || !hasText(candidate.sourceRef())) {
            return PromotionDecision.reject(PromotionRejection.MISSING_SOURCE);
        }
        if (!hasText(candidate.contentFingerprint())
            || !MemoryFingerprint.of(candidate.content()).equals(candidate.contentFingerprint())) {
            return PromotionDecision.reject(PromotionRejection.FINGERPRINT_MISMATCH);
        }
        return PromotionDecision.accept();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
