package com.infotact.polyglotmesh.execution;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ExecutionExceptionHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExecutionExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidExecution(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(ServerWebInputException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Request body must be valid JSON with a language and code."));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleValidationFailure(WebExchangeBindException exception) {
        String details = exception.getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));
        return ResponseEntity.badRequest()
                .body(new ApiError("Request body failed validation: " + details));
    }

    @ExceptionHandler(DataBufferLimitException.class)
    public ResponseEntity<ApiError> handleOversizedRequest(DataBufferLimitException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("Request body exceeds the 128 KB limit."));
    }

    @ExceptionHandler(ExecutionCapacityExceededException.class)
    public ResponseEntity<ApiError> handleExecutionCapacityExceeded(
            ExecutionCapacityExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedFailure(Exception exception) {
        LOGGER.error("Unexpected failure while handling an API request.", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("An unexpected error occurred while processing the request."));
    }

    public record ApiError(String error) {
    }
}
