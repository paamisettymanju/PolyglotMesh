package com.infotact.polyglotmesh.execution;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExecutionConcurrencyLimiter {
    private final Semaphore permits;

    public ExecutionConcurrencyLimiter(
            @Value("${app.execution.max-concurrent:4}") int maxConcurrentExecutions) {
        if (maxConcurrentExecutions < 1) {
            throw new IllegalArgumentException("Execution concurrency limit must be positive.");
        }
        permits = new Semaphore(maxConcurrentExecutions);
    }

    public <T> T execute(Supplier<T> operation) {
        if (!permits.tryAcquire()) {
            throw new ExecutionCapacityExceededException();
        }
        try {
            return operation.get();
        } finally {
            permits.release();
        }
    }
}
