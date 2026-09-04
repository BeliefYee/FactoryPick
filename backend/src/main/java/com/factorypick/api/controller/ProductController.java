package com.factorypick.api.controller;

import com.factorypick.api.domain.*;
import com.factorypick.api.dto.PageResponse;
import com.factorypick.api.service.ProductService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public PageResponse<Product> search(@RequestParam(required=false) String keyword,
                                        @RequestParam(required=false) String category,
                                        @RequestParam(defaultValue="0") int page,
                                        @RequestParam(defaultValue="20") int size) {
        return service.search(keyword, category, page, size);
    }
    @GetMapping("/{id}") public Product detail(@PathVariable long id) { return service.get(id); }
    @GetMapping("/{id}/factories") public List<Factory> factories(@PathVariable long id) { return service.factories(id); }
    @GetMapping("/categories") public List<String> categories() { return service.categories(); }
}
