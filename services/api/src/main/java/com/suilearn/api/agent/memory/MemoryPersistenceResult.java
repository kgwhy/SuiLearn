package com.suilearn.api.agent.memory;

public record MemoryPersistenceResult(PersistenceStatus status, String memoryId, PromotionRejection rejection) {
    public static MemoryPersistenceResult noCandidate() {
        return new MemoryPersistenceResult(PersistenceStatus.NO_CANDIDATE, null, PromotionRejection.NONE);
    }

    public static MemoryPersistenceResult persisted(String id) {
        return new MemoryPersistenceResult(PersistenceStatus.PERSISTED, id, PromotionRejection.NONE);
    }

    public static MemoryPersistenceResult rejected(PromotionRejection rejection) {
        return new MemoryPersistenceResult(PersistenceStatus.REJECTED, null, rejection);
    }

    public static MemoryPersistenceResult failed() {
        return new MemoryPersistenceResult(PersistenceStatus.PERSIST_FAILED, null, PromotionRejection.NONE);
    }
}
