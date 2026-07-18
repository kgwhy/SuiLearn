package com.suilearn.api.agent.controller;

import com.suilearn.api.agent.controller.StudyAgentDtos.AgentError;
import com.suilearn.api.agent.controller.StudyAgentDtos.FieldError;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LearningAgentController.class)
public class LearningAgentExceptionHandler {
    @ExceptionHandler(AgentApiException.class)
    public ResponseEntity<AgentError> handle(AgentApiException exception) {
        AgentErrorCode code = exception.code();
        return ResponseEntity.status(code.status())
            .body(new AgentError(code, code.safeMessage(), null, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AgentError> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), "INVALID", "The field is invalid."))
            .toList();
        AgentErrorCode code = AgentErrorCode.INVALID_AGENT_REQUEST;
        return ResponseEntity.status(code.status())
            .body(new AgentError(code, code.safeMessage(), null, fields));
    }
}
