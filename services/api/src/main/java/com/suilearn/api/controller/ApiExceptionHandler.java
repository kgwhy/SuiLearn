package com.suilearn.api.controller;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.material.application.LegacyMaterialReprocessConflict;
import com.suilearn.api.material.application.MaterialOriginalUnavailableException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AsyncProcessingAdmissionGuard.AsyncProcessingDisabledException.class)
    ResponseEntity<Map<String, String>> handleAsyncProcessingDisabled(
        AsyncProcessingAdmissionGuard.AsyncProcessingDisabledException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(LegacyMaterialReprocessConflict.class)
    ResponseEntity<Map<String, String>> handleLegacyMaterialReprocessConflict(LegacyMaterialReprocessConflict exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "LEGACY_NO_ORIGINAL", "message", exception.getMessage()));
    }

    @ExceptionHandler(MaterialOriginalUnavailableException.class)
    ResponseEntity<Map<String, String>> handleMaterialOriginalUnavailable(MaterialOriginalUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "code", "MATERIAL_ORIGINAL_UNAVAILABLE",
            "reason", "LEGACY_NO_ORIGINAL",
            "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "Request validation failed"));
    }
}
