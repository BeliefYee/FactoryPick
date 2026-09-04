package com.factorypick.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record MapMarkerResponse(long factoryId, String factoryName, String companyName,
                                BigDecimal latitude, BigDecimal longitude, List<String> categories) {}
