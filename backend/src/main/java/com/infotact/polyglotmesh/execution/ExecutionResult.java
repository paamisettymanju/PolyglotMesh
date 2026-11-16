package com.infotact.polyglotmesh.execution;

public record ExecutionResult(
        String language,
        boolean success,
        String output,
        String error,
        long executionTimeMs) {
}
