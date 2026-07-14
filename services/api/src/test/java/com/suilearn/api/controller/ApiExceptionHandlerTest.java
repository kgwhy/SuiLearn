package com.suilearn.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
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
}
