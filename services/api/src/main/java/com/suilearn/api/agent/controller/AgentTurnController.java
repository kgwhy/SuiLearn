package com.suilearn.api.agent.controller;

import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.controller.TurnDtos.ActiveTurnResponse;
import com.suilearn.api.agent.controller.TurnDtos.EventPageResponse;
import com.suilearn.api.agent.controller.TurnDtos.ReplyRequest;
import com.suilearn.api.agent.controller.TurnDtos.StartTurnRequest;
import com.suilearn.api.agent.controller.TurnDtos.TurnControlResponse;
import com.suilearn.api.agent.controller.TurnDtos.TurnEventResponse;
import com.suilearn.api.agent.controller.TurnDtos.TurnResultResponse;
import com.suilearn.api.agent.runtime.Attachment;
import com.suilearn.api.agent.runtime.StartTurnCommand;
import com.suilearn.api.agent.runtime.StreamEvent;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.agent.runtime.TurnResult;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AgentTurnController {
    private final TurnRuntimeService runtime;
    private final AgentConfigurationProperties properties;
    private final Duration syncTimeout;

    @Autowired
    public AgentTurnController(TurnRuntimeService runtime, AgentConfigurationProperties properties) {
        this(runtime, properties, properties.runTimeout());
    }

    AgentTurnController(TurnRuntimeService runtime, AgentConfigurationProperties properties, Duration syncTimeout) {
        this.runtime = runtime;
        this.properties = properties;
        this.syncTimeout = syncTimeout;
    }

    @PostMapping("/api/v2/agent/turns")
    public TurnResultResponse start(@Valid @RequestBody StartTurnRequest request) throws InterruptedException {
        requireEnabled();
        requireScope(request.scope().knowledgeBaseId(), request.scope().materialId());
        var command = new StartTurnCommand(request.learnerId(), request.sessionId(), request.message(),
            request.capability(), new StudyScope(request.scope().knowledgeBaseId(), request.scope().materialId()),
            List.of(), mapAttachments(request.attachments()));
        var outcome = runtime.start(command);
        try {
            TurnResult result = runtime.awaitResult(outcome.record().turnId(), syncTimeout);
            return map(result);
        } catch (TimeoutException timeout) {
            throw new TurnApiException(TurnErrorCode.AGENT_TURN_TIMEOUT);
        }
    }

    @GetMapping("/api/v2/agent/turns/{turnId}/events")
    public EventPageResponse events(
        @PathVariable @Size(max = 128) String turnId,
        @RequestParam(defaultValue = "0") @Min(0) long afterSeq
    ) {
        requireEnabled();
        var page = runtime.eventsAfter(turnId, afterSeq);
        return new EventPageResponse(turnId, page.afterSeq(), page.lastSeq(),
            page.events().stream().map(AgentTurnController::map).toList());
    }

    @PostMapping("/api/v2/agent/turns/{turnId}/cancel")
    public TurnControlResponse cancel(@PathVariable @Size(max = 128) String turnId) {
        requireEnabled();
        var record = runtime.cancel(turnId);
        return new TurnControlResponse(record.turnId(), record.status().name());
    }

    @PostMapping("/api/v2/agent/turns/{turnId}/reply")
    public TurnControlResponse reply(@PathVariable @Size(max = 128) String turnId,
                                     @Valid @RequestBody ReplyRequest request) {
        requireEnabled();
        var record = runtime.submitReply(turnId, request.text(), request.answers());
        return new TurnControlResponse(record.turnId(), record.status().name());
    }

    @GetMapping("/api/v2/agent/sessions/{sessionId}/active-turn")
    public ActiveTurnResponse activeTurn(@PathVariable @Size(max = 128) String sessionId) {
        requireEnabled();
        var info = runtime.checkActiveTurn(sessionId);
        return new ActiveTurnResponse(info.sessionId(), info.turnId(), info.status() == null ? null : info.status().name());
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new TurnApiException(TurnErrorCode.AGENT_FEATURE_DISABLED);
        }
    }

    private static void requireScope(String knowledgeBaseId, String materialId) {
        if ((knowledgeBaseId == null || knowledgeBaseId.isBlank())
            && (materialId == null || materialId.isBlank())) {
            throw new TurnApiException(TurnErrorCode.AGENT_SCOPE_REQUIRED);
        }
    }

    private static List<Attachment> mapAttachments(List<TurnDtos.AttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
            .map(item -> new Attachment(item.attachmentId(), item.mediaType(), item.reference()))
            .toList();
    }

    private static TurnResultResponse map(TurnResult result) {
        return new TurnResultResponse(result.turnId(), result.sessionId(), result.status().name(),
            result.lastSeq(), map(result.terminalEvent()), result.createdAt().toString(), result.finishedAt().toString());
    }

    private static TurnEventResponse map(StreamEvent event) {
        return new TurnEventResponse(event.turnId(), event.sessionId(), event.seq(), event.type().wireName(),
            event.source(), event.stage(), event.content(), event.metadata(), event.ts().toString());
    }
}
