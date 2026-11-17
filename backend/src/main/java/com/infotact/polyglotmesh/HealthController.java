package com.infotact.polyglotmesh;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "PolyglotMesh Backend is Running Successfully!";
    }
}