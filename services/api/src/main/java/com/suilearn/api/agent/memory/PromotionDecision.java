package com.suilearn.api.agent.memory;

public record PromotionDecision(boolean accepted, PromotionRejection reason) {
    public static PromotionDecision accept() {
        return new PromotionDecision(true, PromotionRejection.NONE);
    }

    public static PromotionDecision reject(PromotionRejection reason) {
        return new PromotionDecision(false, reason);
    }
}
