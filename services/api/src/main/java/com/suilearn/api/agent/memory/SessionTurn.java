package com.suilearn.api.agent.memory;

import java.time.Instant;

public record SessionTurn(String summary, String unfinishedGoal, Instant createdAt) {
    public static final int MAX_SUMMARY_LENGTH = 500;
    public static final int MAX_UNFINISHED_GOAL_LENGTH = 200;

    public SessionTurn {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary is required");
        }
        if (summary.length() > MAX_SUMMARY_LENGTH || containsLineBreak(summary)) {
            throw new IllegalArgumentException("summary must be a bounded single-line fact");
        }
        if (unfinishedGoal != null
            && (unfinishedGoal.length() > MAX_UNFINISHED_GOAL_LENGTH || containsLineBreak(unfinishedGoal))) {
            throw new IllegalArgumentException("unfinishedGoal must be bounded single-line text");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }
}
