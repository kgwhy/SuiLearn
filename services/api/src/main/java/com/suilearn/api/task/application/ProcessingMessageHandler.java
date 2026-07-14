package com.suilearn.api.task.application;

import org.springframework.amqp.core.Message;

@FunctionalInterface
interface ProcessingMessageHandler {
    void handle(Message message);
}
