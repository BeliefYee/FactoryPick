package com.factorypick.api.controller;

import com.factorypick.api.dto.StatisticsResponse;
import com.factorypick.api.repository.StatisticsRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsRepository statistics;
    public StatisticsController(StatisticsRepository statistics) { this.statistics = statistics; }
    @GetMapping("/regions") public List<StatisticsResponse> regions() { return statistics.byRegion(); }
    @GetMapping("/categories") public List<StatisticsResponse> categories() { return statistics.byCategory(); }
    @GetMapping("/products") public List<StatisticsResponse> products() { return statistics.byProduct(); }
}
