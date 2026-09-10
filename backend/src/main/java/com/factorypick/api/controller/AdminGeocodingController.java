package com.factorypick.api.controller;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.service.FactoryGeocodingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/factories")
public class AdminGeocodingController {
    private final FactoryGeocodingService service;
    public AdminGeocodingController(FactoryGeocodingService service) { this.service = service; }
    @PostMapping("/{id}/geocode")
    public Factory geocode(@PathVariable long id) { return service.geocode(id); }
}
