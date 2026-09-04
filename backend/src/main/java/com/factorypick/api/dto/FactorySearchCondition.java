package com.factorypick.api.dto;

public record FactorySearchCondition(String keyword, String sido, String sigungu,
                                     String product, String category, int page, int size) {}
