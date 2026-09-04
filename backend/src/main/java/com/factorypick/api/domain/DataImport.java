package com.factorypick.api.domain;

import java.time.LocalDateTime;

public record DataImport(Long importId, String sourceName, int totalRows, int insertedRows,
                         int updatedRows, int skippedRows, int failedRows, String status,
                         String message, LocalDateTime importedAt) {}
