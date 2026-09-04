package com.factorypick.api.service;

import com.factorypick.api.domain.*;
import com.factorypick.api.dto.*;
import com.factorypick.api.repository.*;
import com.factorypick.api.util.CsvParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DataImportService {
    private final FactoryRepository factories;
    private final ProductRepository products;
    private final DataImportRepository imports;
    public DataImportService(FactoryRepository factories, ProductRepository products, DataImportRepository imports) {
        this.factories = factories; this.products = products; this.imports = imports;
    }

    public ImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("CSV 파일을 선택해 주세요.");
        int total = 0, inserted = 0, updated = 0, skipped = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        try {
            List<Map<String, String>> rows = CsvParser.parse(file.getInputStream());
            total = rows.size();
            for (int i = 0; i < rows.size(); i++) {
                try {
                    Map<String, String> row = rows.get(i);
                    FactoryRequest request = factoryRequest(row);
                    Optional<Factory> existing = factories.findExisting(request.businessNumber(), request.factoryName(), request.address());
                    long factoryId;
                    if (existing.isPresent()) {
                        factoryId = existing.get().factoryId(); factories.update(factoryId, request); updated++;
                    } else {
                        factoryId = factories.insert(request); inserted++;
                    }
                    String productName = row.getOrDefault("product_name", "").trim();
                    String category = row.getOrDefault("category", "").trim();
                    if (!productName.isBlank() && !category.isBlank()) {
                        long productId = products.findByNameAndCategory(productName, category)
                                .map(Product::productId)
                                .orElseGet(() -> products.insert(new ProductRequest(productName, category, row.get("product_description"))));
                        products.linkFactoryProduct(factoryId, productId);
                    }
                } catch (Exception e) {
                    failed++;
                    if (errors.size() < 5) errors.add((i + 2) + "행: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("CSV 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        String status = failed == 0 ? "SUCCESS" : (failed == total ? "FAILED" : "PARTIAL");
        String message = errors.isEmpty() ? "처리가 완료되었습니다." : String.join(" / ", errors);
        String source = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.csv");
        long id = imports.insert(source, total, inserted, updated, skipped, failed, status, message);
        return new ImportResult(id, total, inserted, updated, skipped, failed, status, message);
    }

    public List<DataImport> history() { return imports.findAll(); }

    private FactoryRequest factoryRequest(Map<String, String> row) {
        return new FactoryRequest(blank(row.get("business_number")), required(row, "factory_name"),
                required(row, "company_name"), required(row, "address"), required(row, "sido"),
                blank(row.get("sigungu")), decimal(row, "latitude"), decimal(row, "longitude"),
                blank(row.get("industry")), integer(row.get("established_year")),
                blank(row.get("factory_scale")), blank(row.get("phone")));
    }
    private String required(Map<String, String> row, String key) {
        String value = blank(row.get(key));
        if (value == null) throw new IllegalArgumentException(key + " 값이 없습니다.");
        return value;
    }
    private BigDecimal decimal(Map<String, String> row, String key) {
        try { return new BigDecimal(required(row, key)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(key + " 형식이 올바르지 않습니다."); }
    }
    private Integer integer(String value) {
        value = blank(value); if (value == null) return null;
        try { return Integer.valueOf(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("established_year 형식이 올바르지 않습니다."); }
    }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
