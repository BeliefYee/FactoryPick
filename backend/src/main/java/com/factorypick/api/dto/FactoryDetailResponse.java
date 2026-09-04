package com.factorypick.api.dto;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.domain.Product;
import java.util.List;

public record FactoryDetailResponse(Factory factory, List<Product> products) {}
