package com.factorypick.api.controller;

import com.factorypick.api.service.PublicFactoryApiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/factories")
public class PublicFactoryApiController {

    private final PublicFactoryApiService service;

    public PublicFactoryApiController(PublicFactoryApiService service) {
        this.service = service;
    }

    
}