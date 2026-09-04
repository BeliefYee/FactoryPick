package com.factorypick.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FactorySaveRequest(@NotNull @Valid FactoryRequest factory, List<Long> productIds) {}
