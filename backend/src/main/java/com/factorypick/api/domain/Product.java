package com.factorypick.api.domain;

import java.time.LocalDateTime;

public record Product(Long productId, String productName, String category, String description,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {}
