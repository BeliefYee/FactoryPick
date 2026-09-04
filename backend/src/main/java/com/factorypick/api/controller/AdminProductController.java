package com.factorypick.api.controller;

import com.factorypick.api.domain.Product;
import com.factorypick.api.dto.*;
import com.factorypick.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService service;
    public AdminProductController(ProductService service) { this.service = service; }

    @GetMapping
    public PageResponse<Product> search(@RequestParam(required=false) String keyword,
                                        @RequestParam(required=false) String category,
                                        @RequestParam(defaultValue="0") int page,
                                        @RequestParam(defaultValue="20") int size) {
        return service.search(keyword, category, page, size);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
