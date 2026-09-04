package com.factorypick.api.dto;

public record ImportResult(long importId, int totalRows, int insertedRows, int updatedRows,
                           int skippedRows, int failedRows, String status, String message) {}
