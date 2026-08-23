package com.suilearn.api.agent.runtime;

import java.util.List;
import java.util.Optional;

public interface TurnStore {
    TurnRecord createTurn(TurnContext context, String inputMessageId, StreamEvent firstEvent);

    Optional<TurnRecord> findTurn(String turnId);

    Optional<TurnRecord> findActiveTurn(String sessionId);

    List<StreamEvent> findEventsAfter(String turnId, long afterSeq);

    Optional<StreamEvent> findLastEvent(String turnId);

    TurnRecord appendEvent(StreamEvent event);

    TurnRecord appendTerminalEvent(StreamEvent event, TurnStatus terminalStatus);

    TurnRecord updateStatus(String turnId, TurnStatus status);

    List<TurnRecord> findOrphanedRunning();

    long countEvents(String turnId);
}
