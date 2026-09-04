package com.factorypick.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FactoryRequest(
        @Size(max = 30) String businessNumber,
        @NotBlank @Size(max = 150) String factoryName,
        @NotBlank @Size(max = 150) String companyName,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 50) String sido,
        @Size(max = 80) String sigungu,
        @NotNull @DecimalMin("33.0") @DecimalMax("39.0") BigDecimal latitude,
        @NotNull @DecimalMin("124.0") @DecimalMax("132.0") BigDecimal longitude,
        @Size(max = 100) String industry,
        @Min(1800) @Max(2100) Integer establishedYear,
        @Size(max = 50) String factoryScale,
        @Size(max = 30) String phone
) {}
