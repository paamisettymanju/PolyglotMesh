package com.infotact.polyglotmesh.execution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExecutionRequest(
        @NotBlank(message = "language is required.")
        @Pattern(regexp = "(?i)python|javascript|js",
                message = "language must be python, javascript, or js.")
        String language,
        @NotNull(message = "code is required.")
        @Size(max = PolyglotExecutionService.MAX_SOURCE_LENGTH,
                message = "code must not exceed 50000 characters.")
        String code) {
}
