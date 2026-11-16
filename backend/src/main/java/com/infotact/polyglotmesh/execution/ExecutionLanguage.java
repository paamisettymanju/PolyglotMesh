package com.infotact.polyglotmesh.execution;

import java.util.Locale;

public enum ExecutionLanguage {
    PYTHON("python", "python"),
    JAVASCRIPT("javascript", "js");

    private final String id;
    private final String polyglotId;

    ExecutionLanguage(String id, String polyglotId) {
        this.id = id;
        this.polyglotId = polyglotId;
    }

    public String id() {
        return id;
    }

    String polyglotId() {
        return polyglotId;
    }

    public static ExecutionLanguage from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Language must be python or javascript.");
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "python" -> PYTHON;
            case "javascript", "js" -> JAVASCRIPT;
            default -> throw new IllegalArgumentException("Language must be python or javascript.");
        };
    }
}
