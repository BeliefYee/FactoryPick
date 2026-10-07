package com.factorypick.api.dto;

import com.factorypick.api.domain.Factory;
import java.util.List;

public record FactoryDetailResponse(Factory factory, List<String> categories) {}
