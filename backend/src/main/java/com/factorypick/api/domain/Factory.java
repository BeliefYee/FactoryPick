package com.factorypick.api.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

public record Factory(
        Long factoryId, String businessNumber, String factoryName, String companyName,
        String address, String sido, String sigungu, BigDecimal latitude, BigDecimal longitude,
        String industry, Integer establishedYear, String factoryScale, String phone,
        LocalDateTime createdAt, LocalDateTime updatedAt,
        String factoryManageNo, String representativeName, String managingAgencyName, String faxNumber,
        Integer employeeCount, LocalDate firstRegisteredDate, String primaryIndustryCode,
        @com.fasterxml.jackson.annotation.JsonIgnore String mainProductText, String homepageRaw, String industrialComplexName,
        String geocodingStatus, LocalDateTime geocodedAt, LocalDateTime lastSyncedAt
) {}
