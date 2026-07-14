package com.suilearn.api.task.application;

enum OutboxDeliveryState {
    PENDING,
    RETRY_WAIT,
    PUBLISHED,
    DEAD_LETTER
}
