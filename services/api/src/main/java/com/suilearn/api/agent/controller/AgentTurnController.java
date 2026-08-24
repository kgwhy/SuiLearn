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
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.StartTurnCommand;
import com.suilearn.api.agent.runtime.StreamEvent;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.agent.runtime.TurnResult;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
import com.suilearn.api.security.AgentAuthProperties;
import com.suilearn.api.security.LearnerPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
    private final AgentAuthProperties authProperties;

    public AgentTurnController(TurnRuntimeService runtime, AgentConfigurationProperties properties) {
        this(runtime, properties, properties.runTimeout());
    }

    @Autowired
    public AgentTurnController(TurnRuntimeService runtime, AgentConfigurationProperties properties,
                               AgentAuthProperties authProperties) {
        this(runtime, properties, properties.runTimeout(), authProperties);
    }

    AgentTurnController(TurnRuntimeService runtime, AgentConfigurationProperties properties, Duration syncTimeout) {
        this(runtime, properties, syncTimeout, new AgentAuthProperties());
    }

    public AgentTurnController(TurnRuntimeService runtime, AgentConfigurationProperties properties,
                               Duration syncTimeout, AgentAuthProperties authProperties) {
        this.runtime = runtime;
        this.properties = properties;
        this.syncTimeout = syncTimeout;
        this.authProperties = authProperties;
    }

    @PostMapping("/api/v2/agent/turns")
    public TurnResultResponse start(@Valid @RequestBody StartTurnRequest request,
                                  Authentication authentication) throws InterruptedException {
        requireEnabled();
        requireScope(request.scope().knowledgeBaseId(), request.scope().materialId());
        var command = new StartTurnCommand(learnerId(request.learnerId(), authentication), request.sessionId(), request.message(),
            request.capability(), new StudyScope(request.scope().knowledgeBaseId(), request.scope().materialId()),
            List.of(), mapAttachments(request.attachments()));
        var outcome = runtime.start(command);
        try {
            TurnResult result = runtime.awaitResult(outcome.record().turnId(), syncTimeout);
            return map(result, runtime.eventsAfter(result.turnId(), 0).events());
        } catch (TimeoutException timeout) {
            throw new TurnApiException(TurnErrorCode.AGENT_TURN_TIMEOUT);
        }
    }

    @GetMapping("/api/v2/agent/turns/{turnId}/events")
    public EventPageResponse events(
        @PathVariable @Size(max = 128) String turnId,
        @RequestParam(defaultValue = "0") @Min(0) long afterSeq,
        Authentication authentication
    ) {
        requireEnabled();
        var page = runtime.eventsAfter(turnId, afterSeq, scopedLearnerId(authentication));
        return new EventPageResponse(turnId, page.afterSeq(), page.lastSeq(),
            page.events().stream().map(AgentTurnController::map).toList());
    }

    @PostMapping("/api/v2/agent/turns/{turnId}/cancel")
    public TurnControlResponse cancel(@PathVariable @Size(max = 128) String turnId,
                                    Authentication authentication) {
        requireEnabled();
        var record = runtime.cancel(turnId, scopedLearnerId(authentication));
        return new TurnControlResponse(record.turnId(), record.status().name());
    }

    @PostMapping("/api/v2/agent/turns/{turnId}/reply")
    public TurnControlResponse reply(@PathVariable @Size(max = 128) String turnId,
                                     @Valid @RequestBody ReplyRequest request,
                                     Authentication authentication) {
        requireEnabled();
        var record = runtime.submitReply(turnId, request.text(), request.answers(), scopedLearnerId(authentication));
        return new TurnControlResponse(record.turnId(), record.status().name());
    }

    @GetMapping("/api/v2/agent/sessions/{sessionId}/active-turn")
    public ActiveTurnResponse activeTurn(@PathVariable @Size(max = 128) String sessionId,
                                       Authentication authentication) {
        requireEnabled();
        var info = runtime.checkActiveTurn(sessionId, scopedLearnerId(authentication));
        return new ActiveTurnResponse(info.sessionId(), info.turnId(), info.status() == null ? null : info.status().name());
    }

    private String learnerId(String requested, Authentication authentication) {
        String principalLearnerId = scopedLearnerId(authentication);
        return principalLearnerId == null ? requested : principalLearnerId;
    }

    private String scopedLearnerId(Authentication authentication) {
        if (!authProperties.isEnabled()) {
            return null;
        }
        LearnerPrincipal principal = LearnerPrincipal.fromAuthentication(authentication);
        if (principal == null) {
            throw new TurnApiException(TurnErrorCode.AGENT_AUTH_REQUIRED);
        }
        return principal.learnerId();
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

    private static TurnResultResponse map(TurnResult result, List<StreamEvent> events) {
        StreamEvent terminal = result.terminalEvent();
        StreamEvent usageSource = events == null ? terminal : events.stream()
            .filter(event -> event.type() == EventType.RESULT)
            .reduce((first, second) -> second)
            .orElse(terminal);
        return new TurnResultResponse(result.turnId(), result.sessionId(), result.status().name(),
            result.lastSeq(), map(terminal), result.createdAt().toString(), result.finishedAt().toString(),
            metadataLong(usageSource, "promptTokens"), metadataLong(usageSource, "completionTokens"),
            metadataDouble(usageSource, "usageCostUsd"), (int) metadataLong(usageSource, "toolCalls"),
            (int) metadataLong(usageSource, "estimatedContextTokens"));
    }

    private static long metadataLong(StreamEvent event, String key) {
        Object value = event.metadata().get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static double metadataDouble(StreamEvent event, String key) {
        Object value = event.metadata().get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static TurnEventResponse map(StreamEvent event) {
        return new TurnEventResponse(event.turnId(), event.sessionId(), event.seq(), event.type().wireName(),
            event.source(), event.stage(), event.content(), event.metadata(), event.ts().toString());
    }
}
