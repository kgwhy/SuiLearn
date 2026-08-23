package com.suilearn.api.agent.runtime;

@FunctionalInterface
public interface TurnExecutor {
    void execute(TurnContext context, TurnEventSink events);
}
