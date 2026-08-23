package com.suilearn.api.agent.context;

import com.suilearn.api.agent.infrastructure.turn.SessionMessageEntity;
import com.suilearn.api.agent.infrastructure.turn.SessionMessageJpaRepository;
import com.suilearn.api.agent.infrastructure.turn.SessionSummaryEntity;
import com.suilearn.api.agent.infrastructure.turn.SessionSummaryJpaRepository;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.llm.LlmRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RollingSessionSummary {
    private final SessionMessageJpaRepository messages;
    private final SessionSummaryJpaRepository summaries;
    private final LlmClient client;
    private final Clock clock;
    private final String model;
    private final int windowSize;

    public RollingSessionSummary(SessionMessageJpaRepository messages, SessionSummaryJpaRepository summaries,
                                 LlmClient client, Clock clock, String model, int windowSize) {
        this.messages = messages;
        this.summaries = summaries;
        this.client = client;
        this.clock = clock;
        this.model = model == null || model.isBlank() ? "suilearn-default" : model;
        this.windowSize = Math.max(2, windowSize);
    }

    public Optional<String> ensure(String sessionId, String currentTurnId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        List<SessionMessageEntity> history = messages.findBySessionIdAndTurnIdNotOrderByCreatedAtAsc(
            sessionId, currentTurnId);
        Optional<SessionSummaryEntity> existing = summaries.findById(sessionId);
        if (history.isEmpty()) {
            return existing.map(SessionSummaryEntity::getSummary);
        }
        try {
            List<SessionMessageEntity> batch;
            String oldSummary;
            if (existing.isPresent() && history.size() <= windowSize / 2) {
                // Anti-drift: rebuild from original messages when the retained window is small.
                batch = history;
                oldSummary = "";
            } else if (existing.isPresent()) {
                Instant watermark = existing.get().getSummaryUpToCreatedAt();
                batch = history.stream().filter(message -> watermark == null
                    || message.getCreatedAt().isAfter(watermark)).toList();
                if (batch.isEmpty()) {
                    return existing.map(SessionSummaryEntity::getSummary);
                }
                oldSummary = existing.get().getSummary();
            } else {
                int from = Math.max(0, history.size() - windowSize);
                batch = new ArrayList<>(history.subList(from, history.size()));
                oldSummary = "";
            }
            String newSummary = summarize(oldSummary, batch);
            SessionMessageEntity last = batch.get(batch.size() - 1);
            Instant now = clock.instant();
            summaries.save(new SessionSummaryEntity(sessionId, newSummary, last.getId(),
                last.getCreatedAt(), now));
            return Optional.of(newSummary);
        } catch (RuntimeException summaryFailure) {
            return existing.map(SessionSummaryEntity::getSummary);
        }
    }

    private String summarize(String oldSummary, List<SessionMessageEntity> batch) {
        String conversation = batch.stream()
            .map(message -> message.getRole() + ": " + message.getContent())
            .reduce("", (left, right) -> left.isBlank() ? right : left + "\n" + right);
        String user = oldSummary == null || oldSummary.isBlank()
            ? "Summarize this learning conversation for future turns. Keep goals, preferences, weak points, and mastery.\n\n"
                + conversation
            : "Existing summary:\n" + oldSummary + "\n\nNew messages:\n" + conversation
                + "\n\nUpdate the summary without losing earlier facts.";
        var response = client.chat(new LlmRequest(model, List.of(
            LlmMessage.system("You are SuiLearn's session summarizer. Return only the concise summary."),
            LlmMessage.user(user)), List.of(), 0.1, null));
        if (response.content() == null || response.content().isBlank()) {
            throw new IllegalStateException("session summarizer returned empty summary");
        }
        return response.content().strip();
    }
}
