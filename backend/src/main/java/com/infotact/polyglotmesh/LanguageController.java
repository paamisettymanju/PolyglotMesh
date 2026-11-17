package com.infotact.polyglotmesh;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LanguageController {

    @GetMapping("/api/languages")
    public List<String> getLanguages() {
        return List.of("Python", "JavaScript");
    }
}