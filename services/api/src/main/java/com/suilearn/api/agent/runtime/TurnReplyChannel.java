package com.suilearn.api.agent.runtime;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface TurnReplyChannel {
    TurnReply awaitReply(String turnId, Duration timeout) throws InterruptedException, TimeoutException;
}
