package com.suilearn.api.agent.memory;

public record LayerDeletion(DeletionStatus status, long deletedCount) {
    public static LayerDeletion succeeded(long count) {
        return new LayerDeletion(DeletionStatus.SUCCEEDED, count);
    }

    public static LayerDeletion failed() {
        return new LayerDeletion(DeletionStatus.FAILED, 0);
    }
}
