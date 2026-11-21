package com.infotact.polyglotmesh.execution;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ExecutionExceptionHandlerTest {
    private final ExecutionExceptionHandler handler = new ExecutionExceptionHandler();

    @Test
    void returnsStableInternalServerErrorWithoutExposingExceptionDetails() {
        ResponseEntity<ExecutionExceptionHandler.ApiError> response =
                handler.handleUnexpectedFailure(
                        new IllegalStateException("private implementation detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isEqualTo(new ExecutionExceptionHandler.ApiError(
                        "An unexpected error occurred while processing the request."));
    }

    @Test
    void mapsExecutionCapacityRejectionsToTooManyRequests() {
        ResponseEntity<ExecutionExceptionHandler.ApiError> response =
                handler.handleExecutionCapacityExceeded(
                        new ExecutionCapacityExceededException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody())
                .isEqualTo(new ExecutionExceptionHandler.ApiError(
                        "Execution capacity is full. Try again shortly."));
    }
}
