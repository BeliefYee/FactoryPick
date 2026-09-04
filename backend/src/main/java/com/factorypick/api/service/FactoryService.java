package com.factorypick.api.service;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.dto.*;
import com.factorypick.api.exception.NotFoundException;
import com.factorypick.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FactoryService {
    private final FactoryRepository factories;
    private final ProductRepository products;
    public FactoryService(FactoryRepository factories, ProductRepository products) {
        this.factories = factories; this.products = products;
    }

    public PageResponse<Factory> search(String keyword, String sido, String sigungu, String product,
                                        String category, int page, int size) {
        validatePage(page, size);
        var condition = new FactorySearchCondition(keyword, sido, sigungu, product, category, page, size);
        return PageResponse.of(factories.search(condition), page, size, factories.count(condition));
    }

    public FactoryDetailResponse detail(long id) {
        Factory factory = factories.findById(id).orElseThrow(() -> new NotFoundException("공장을 찾을 수 없습니다."));
        return new FactoryDetailResponse(factory, products.findByFactoryId(id));
    }

    @Transactional
    public FactoryDetailResponse create(FactoryRequest request, List<Long> productIds) {
        long id = factories.insert(request);
        products.replaceFactoryProducts(id, productIds == null ? List.of() : productIds);
        return detail(id);
    }

    @Transactional
    public FactoryDetailResponse update(long id, FactoryRequest request, List<Long> productIds) {
        if (factories.update(id, request) == 0) throw new NotFoundException("공장을 찾을 수 없습니다.");
        if (productIds != null) products.replaceFactoryProducts(id, productIds);
        return detail(id);
    }

    public void delete(long id) {
        if (factories.delete(id) == 0) throw new NotFoundException("공장을 찾을 수 없습니다.");
    }

    private void validatePage(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        if (size < 1 || size > 500) throw new IllegalArgumentException("size는 1~500이어야 합니다.");
    }
}
