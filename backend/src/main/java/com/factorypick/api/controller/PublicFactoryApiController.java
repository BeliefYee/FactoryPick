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

    @GetMapping("/import")
    public String importFactories(
            @RequestParam String industrialComplexName) {

        service.importFactories(industrialComplexName);

        return "공장 데이터 가져오기 완료: " + industrialComplexName;
    }
}