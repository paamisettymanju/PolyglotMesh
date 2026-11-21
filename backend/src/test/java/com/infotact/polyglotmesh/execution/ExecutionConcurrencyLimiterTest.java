package com.infotact.polyglotmesh.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ExecutionConcurrencyLimiterTest {
    @Test
    void rejectsExcessExecutionsAndReleasesCapacityAfterCompletion() throws Exception {
        ExecutionConcurrencyLimiter limiter = new ExecutionConcurrencyLimiter(1);
        CountDownLatch executionStarted = new CountDownLatch(1);
        CountDownLatch allowExecutionToFinish = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<String> firstExecution = executor.submit(() -> limiter.execute(() -> {
                executionStarted.countDown();
                try {
                    if (!allowExecutionToFinish.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to finish test execution.");
                    }
                    return "finished";
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Test execution was interrupted.", exception);
                }
            }));

            assertThat(executionStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> limiter.execute(() -> "should not run"))
                    .isInstanceOf(ExecutionCapacityExceededException.class)
                    .hasMessage("Execution capacity is full. Try again shortly.");

            allowExecutionToFinish.countDown();
            assertThat(firstExecution.get(5, TimeUnit.SECONDS)).isEqualTo("finished");
            assertThat(limiter.execute(() -> "capacity restored")).isEqualTo("capacity restored");
        } finally {
            allowExecutionToFinish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsNonPositiveConcurrencyLimits() {
        assertThatThrownBy(() -> new ExecutionConcurrencyLimiter(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Execution concurrency limit must be positive.");
    }
}
