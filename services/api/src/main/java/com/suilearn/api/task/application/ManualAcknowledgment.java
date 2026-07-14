package com.suilearn.api.task.application;

@FunctionalInterface
interface ManualAcknowledgment {
    void acknowledge(String messageId);
}
