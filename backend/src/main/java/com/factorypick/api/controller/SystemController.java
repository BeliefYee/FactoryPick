package com.factorypick.api.controller;

import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemController {
    @GetMapping("/health")
    public Map<String, Object> health() { return Map.of("status", "UP", "timestamp", Instant.now()); }
}
