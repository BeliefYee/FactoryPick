package com.factorypick.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(@NotBlank @Size(max = 150) String productName,
                             @NotBlank @Size(max = 100) String category,
                             @Size(max = 500) String description) {}
