package com.factorypick.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FactoryRequest(
        @Size(max = 30) String businessNumber,
        @NotBlank @Size(max = 255) String factoryName,
        @Size(max = 150) String companyName,
        @Size(max = 1000) String address,
        @Size(max = 50) String sido,
        @Size(max = 80) String sigungu,
        @DecimalMin("33.0") @DecimalMax("39.0") BigDecimal latitude,
        @DecimalMin("124.0") @DecimalMax("132.0") BigDecimal longitude,
        @Size(max = 10000) String industry,
        @Min(1800) @Max(2100) Integer establishedYear,
        @Size(max = 50) String factoryScale,
        @Size(max = 100) String phone
) {
    @AssertTrue(message = "위도와 경도는 함께 입력하거나 둘 다 비워야 합니다.")
    public boolean isCoordinatePairValid() { return (latitude == null) == (longitude == null); }
}
