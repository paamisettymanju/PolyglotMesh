package com.infotact.polyglotmesh.execution;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.IOAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Source;
import org.springframework.stereotype.Service;

@Service
public class PolyglotExecutionService {
    public static final int MAX_SOURCE_LENGTH = 50_000;

    private static final int MAX_OUTPUT_BYTES = 64_000;
    private static final int MAX_ERROR_LENGTH = 2_000;
    private static final long MAX_STATEMENTS = 100_000;
    private static final ResourceLimits RESOURCE_LIMITS = ResourceLimits.newBuilder()
            .statementLimit(MAX_STATEMENTS, source -> true)
            .build();

    public ExecutionResult execute(String requestedLanguage, String code) {
        ExecutionLanguage language = ExecutionLanguage.from(requestedLanguage);
        if (code == null) {
            throw new IllegalArgumentException("Code must not be null.");
        }
        if (code.length() > MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException(
                    "Code exceeds the " + MAX_SOURCE_LENGTH + " character limit.");
        }

        long startedAt = System.nanoTime();
        BoundedOutputStream output = new BoundedOutputStream(MAX_OUTPUT_BYTES);
        BoundedOutputStream errorOutput = new BoundedOutputStream(MAX_OUTPUT_BYTES);
        String error = null;

        try (Context context = Context.newBuilder(language.polyglotId())
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(className -> false)
                .allowIO(IOAccess.NONE)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .resourceLimits(RESOURCE_LIMITS)
                .out(output)
                .err(errorOutput)
                .build()) {
            context.eval(Source.newBuilder(language.polyglotId(), code, "user-code").buildLiteral());
        } catch (PolyglotException exception) {
            error = exception.isResourceExhausted()
                    ? "Execution exceeded the resource limit."
                    : safeMessage(exception.getMessage());
        }

        String diagnostics = errorOutput.text();
        if (!diagnostics.isBlank()) {
            error = error == null ? diagnostics : error + "\n" + diagnostics;
        }
        if (output.wasTruncated() || errorOutput.wasTruncated()) {
            String limitMessage = "Output exceeded the " + MAX_OUTPUT_BYTES + " byte limit.";
            error = error == null ? limitMessage : error + "\n" + limitMessage;
        }

        return new ExecutionResult(
                language.id(),
                error == null,
                output.text(),
                error,
                (System.nanoTime() - startedAt) / 1_000_000);
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Execution failed.";
        }
        String normalized = message.strip();
        return normalized.length() > MAX_ERROR_LENGTH
                ? normalized.substring(0, MAX_ERROR_LENGTH) + "…"
                : normalized;
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final int limit;
        private final ByteArrayOutputStream buffer;
        private boolean truncated;

        private BoundedOutputStream(int limit) {
            this.limit = limit;
            this.buffer = new ByteArrayOutputStream(Math.min(limit, 1024));
        }

        @Override
        public void write(int value) {
            if (buffer.size() >= limit) {
                truncated = true;
                return;
            }
            buffer.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            int remaining = limit - buffer.size();
            int accepted = Math.min(remaining, length);
            if (accepted > 0) {
                buffer.write(bytes, offset, accepted);
            }
            if (accepted < length) {
                truncated = true;
            }
        }

        private String text() {
            return buffer.toString(StandardCharsets.UTF_8);
        }

        private boolean wasTruncated() {
            return truncated;
        }
    }
}
