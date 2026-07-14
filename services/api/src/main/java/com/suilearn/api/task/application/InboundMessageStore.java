package com.suilearn.api.task.application;

interface InboundMessageStore {
    boolean claim(String messageId);
    void complete(String messageId);
}
