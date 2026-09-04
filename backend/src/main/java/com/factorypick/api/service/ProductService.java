package com.factorypick.api.service;

import com.factorypick.api.domain.*;
import com.factorypick.api.dto.*;
import com.factorypick.api.exception.NotFoundException;
import com.factorypick.api.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository products;
    public ProductService(ProductRepository products) { this.products = products; }

    public PageResponse<Product> search(String keyword, String category, int page, int size) {
        if (page < 0 || size < 1 || size > 500) throw new IllegalArgumentException("page 또는 size 값이 올바르지 않습니다.");
        return PageResponse.of(products.search(keyword, category, page, size), page, size,
                products.count(keyword, category));
    }
    public Product get(long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다."));
    }
    public List<Factory> factories(long id) { get(id); return products.findFactoriesByProduct(id); }
    public List<String> categories() { return products.categories(); }
    public Product create(ProductRequest request) { return get(products.insert(request)); }
    public Product update(long id, ProductRequest request) {
        if (products.update(id, request) == 0) throw new NotFoundException("제품을 찾을 수 없습니다.");
        return get(id);
    }
    public void delete(long id) {
        if (products.delete(id) == 0) throw new NotFoundException("제품을 찾을 수 없습니다.");
    }
}
