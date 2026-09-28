package com.factorypick.api.controller;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.domain.Product;
import com.factorypick.api.dto.*;
import com.factorypick.api.repository.FactoryRepository;
import com.factorypick.api.service.FactoryService;
import com.factorypick.api.service.PublicFactoryApiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/factories")
public class FactoryController {

    private final FactoryService service;
    private final FactoryRepository repository;
    private final PublicFactoryApiService publicFactoryApiService;

    public FactoryController(
            FactoryService service,
            FactoryRepository repository,
            PublicFactoryApiService publicFactoryApiService
    ) {
        this.service = service;
        this.repository = repository;
        this.publicFactoryApiService = publicFactoryApiService;
    }

    @GetMapping
    public PageResponse<Factory> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        return service.search(
                keyword,
                sido,
                sigungu,
                product,
                category,
                page,
                size
        );
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return repository.categories();
    }

    @GetMapping("/{id}")
    public FactoryDetailResponse detail(
            @PathVariable long id
    ) {
        return service.detail(id);
    }

    @GetMapping("/{id}/products")
    public List<Product> products(
            @PathVariable long id
    ) {
        return service.detail(id).products();
    }

    @GetMapping("/markers")
    public List<MapMarkerResponse> markers(
            @RequestParam(defaultValue = "33.0") double south,
            @RequestParam(defaultValue = "124.0") double west,
            @RequestParam(defaultValue = "39.0") double north,
            @RequestParam(defaultValue = "132.0") double east,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String category
    ) {

        if (south > north || west > east) {
            throw new IllegalArgumentException(
                    "지도 영역 좌표가 올바르지 않습니다."
            );
        }

        return repository.markers(
                south,
                west,
                north,
                east,
                new FactorySearchCondition(
                        keyword,
                        sido,
                        sigungu,
                        product,
                        category,
                        0,
                        20
                )
        );
    }

    @PostMapping("/import")
    public Map<String, Object> importFactories(
            @RequestParam String industrialComplexName
    ) {

        publicFactoryApiService.importFactories(
                industrialComplexName
        );

        return Map.of(
                "success", true,
                "message", "공장 데이터 연동이 완료되었습니다."
        );
    }
}
