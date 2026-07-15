package com.suilearn.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {
    @Test
    void mapsDisabledAsyncProcessingToAStableServiceUnavailableResponse() {
        var response = new ApiExceptionHandler().handleAsyncProcessingDisabled(
            new AsyncProcessingAdmissionGuard.AsyncProcessingDisabledException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry(
            "message", "ASYNC_PROCESSING_DISABLED: new material uploads and imports are unavailable"
        );
    }

    @Test
    void mapsLegacyOriginalAbsenceToTheExactOriginalUnavailableErrorContract() throws Exception {
        var exceptionType = Class.forName("com.suilearn.api.material.application.MaterialOriginalUnavailableException");
        var exception = (RuntimeException) exceptionType.getConstructor().newInstance();
        Method handler = Arrays.stream(ApiExceptionHandler.class.getDeclaredMethods())
            .filter(method -> Arrays.asList(method.getParameterTypes()).contains(exceptionType))
            .findFirst()
            .orElseThrow();

        @SuppressWarnings("unchecked")
        var response = (org.springframework.http.ResponseEntity<java.util.Map<String, String>>) handler.invoke(
            new ApiExceptionHandler(), exception
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
            "code", "MATERIAL_ORIGINAL_UNAVAILABLE",
            "reason", "LEGACY_NO_ORIGINAL",
            "message", "The original file is unavailable for this legacy material."
        ));
    }
}
