package com.suilearn.api.agent.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public final class TurnDtos {
    private TurnDtos() {}

    public record StartTurnRequest(
        @NotBlank @Size(max = 128) String learnerId,
        @Size(max = 128) String sessionId,
        @NotBlank @Size(max = 8000) String message,
        @Size(max = 64) String capability,
        @NotNull @Valid ScopeRequest scope,
        @Size(max = 8) List<@Valid AttachmentRequest> attachments
    ) {}

    public record ScopeRequest(
        @Size(max = 128) String knowledgeBaseId,
        @Size(max = 128) String materialId
    ) {}

    public record AttachmentRequest(
        @NotBlank @Size(max = 128) String attachmentId,
        @Size(max = 128) String mediaType,
        @NotBlank @Size(max = 512) String reference
    ) {}

    public record ReplyRequest(
        @Size(max = 8000) String text,
        Map<String, Object> answers
    ) {}

    public record TurnEventResponse(
        String turnId,
        String sessionId,
        long seq,
        String type,
        String source,
        String stage,
        String content,
        Map<String, Object> metadata,
        String ts
    ) {}

    public record EventPageResponse(
        String turnId,
        long afterSeq,
        long lastSeq,
        List<TurnEventResponse> events
    ) {
        public EventPageResponse {
            events = List.copyOf(events == null ? List.of() : events);
        }
    }

    public record TurnResultResponse(
        String turnId,
        String sessionId,
        String status,
        long lastSeq,
        TurnEventResponse terminalEvent,
        String createdAt,
        String finishedAt,
        long promptTokens,
        long completionTokens,
        double usageCostUsd,
        int actionTraceCount,
        int estimatedContextTokens
    ) {}

    public record TurnControlResponse(String turnId, String status) {}

    public record ActiveTurnResponse(String sessionId, String turnId, String status) {}

    public record AgentTurnError(
        String code,
        String message,
        String correlationId,
        List<FieldError> fieldErrors
    ) {
        public AgentTurnError {
            fieldErrors = List.copyOf(fieldErrors == null ? List.of() : fieldErrors);
        }
    }

    public record FieldError(String field, String code, String message) {}
}
