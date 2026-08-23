package com.suilearn.api.agent.controller;

import com.suilearn.api.agent.controller.TurnDtos.AgentTurnError;
import com.suilearn.api.agent.controller.TurnDtos.FieldError;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AgentTurnController.class, AgentCapabilitiesController.class})
public class AgentTurnExceptionHandler {
    @ExceptionHandler(TurnApiException.class)
    public ResponseEntity<AgentTurnError> handle(TurnApiException exception) {
        TurnErrorCode code = exception.code();
        return ResponseEntity.status(code.status())
            .body(new AgentTurnError(code.name(), code.safeMessage(), null, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AgentTurnError> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), "INVALID", "The field is invalid."))
            .toList();
        return ResponseEntity.status(TurnErrorCode.INVALID_AGENT_REQUEST.status())
            .body(new AgentTurnError(TurnErrorCode.INVALID_AGENT_REQUEST.name(),
                TurnErrorCode.INVALID_AGENT_REQUEST.safeMessage(), null, fields));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AgentTurnError> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(TurnErrorCode.INVALID_AGENT_REQUEST.status())
            .body(new AgentTurnError(TurnErrorCode.INVALID_AGENT_REQUEST.name(),
                TurnErrorCode.INVALID_AGENT_REQUEST.safeMessage(), null, List.of()));
    }
}
