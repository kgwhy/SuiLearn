package com.suilearn.api.agent.runtime;

@FunctionalInterface
public interface TurnEventListener {
    void onEvent(StreamEvent event);
}
