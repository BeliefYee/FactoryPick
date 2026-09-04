package com.factorypick.api.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Factory(
        Long factoryId, String businessNumber, String factoryName, String companyName,
        String address, String sido, String sigungu, BigDecimal latitude, BigDecimal longitude,
        String industry, Integer establishedYear, String factoryScale, String phone,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {}
