package com.factorypick.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record FactorySaveRequest(@NotNull @Valid FactoryRequest factory) {}
