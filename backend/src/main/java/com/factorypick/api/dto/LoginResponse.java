package com.factorypick.api.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt) {}
