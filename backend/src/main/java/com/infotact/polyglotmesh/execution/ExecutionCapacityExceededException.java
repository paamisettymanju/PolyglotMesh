package com.infotact.polyglotmesh.execution;

public class ExecutionCapacityExceededException extends RuntimeException {
    public ExecutionCapacityExceededException() {
        super("Execution capacity is full. Try again shortly.");
    }
}
