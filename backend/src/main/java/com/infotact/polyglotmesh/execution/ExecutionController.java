package com.infotact.polyglotmesh.execution;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api")
public class ExecutionController {
    private final PolyglotExecutionService executionService;

    public ExecutionController(PolyglotExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/execute")
    public Mono<ExecutionResult> execute(@RequestBody ExecutionRequest request) {
        return Mono.fromCallable(
                        () -> executionService.execute(request.language(), request.code()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
