package com.factorypick.api.controller;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.dto.*;
import com.factorypick.api.service.FactoryService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/factories")
public class AdminFactoryController {
    private final FactoryService service;
    public AdminFactoryController(FactoryService service) { this.service = service; }

    @GetMapping
    public PageResponse<Factory> search(@RequestParam(required=false) String keyword,
                                        @RequestParam(required=false) String sido,
                                        @RequestParam(required=false) String sigungu,
                                        @RequestParam(required=false) String product,
                                        @RequestParam(required=false) String category,
                                        @RequestParam(defaultValue="0") int page,
                                        @RequestParam(defaultValue="20") int size) {
        return service.search(keyword, sido, sigungu, product, category, page, size);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FactoryDetailResponse create(@Valid @RequestBody FactorySaveRequest request) {
        return service.create(request.factory(), request.productIds());
    }
    @PutMapping("/{id}")
    public FactoryDetailResponse update(@PathVariable long id, @Valid @RequestBody FactorySaveRequest request) {
        return service.update(id, request.factory(), request.productIds());
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
