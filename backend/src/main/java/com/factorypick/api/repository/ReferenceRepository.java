package com.factorypick.api.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

/** Resolves API names to the normalized database identifiers. */
@Repository
public class ReferenceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public ReferenceRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Long company(String name) {
        if (name == null || name.isBlank()) return null;
        var p = Map.of("name", name.trim());
        jdbc.update("INSERT INTO company(company_name) VALUES(:name) ON DUPLICATE KEY UPDATE company_name=company_name", p);
        return jdbc.queryForObject("SELECT company_id FROM company WHERE company_name=:name", p, Long.class);
    }

    public Long region(String sido, String sigungu) {
        if (sido == null || sido.isBlank()) return null;
        var p = Map.of("sido", sido.trim(), "sigungu", sigungu == null ? "" : sigungu.trim());
        jdbc.update("INSERT INTO region(sido_name,sigungu_name) VALUES(:sido,:sigungu) ON DUPLICATE KEY UPDATE sido_name=sido_name", p);
        return jdbc.queryForObject("SELECT region_id FROM region WHERE sido_name=:sido AND sigungu_name=:sigungu", p, Long.class);
    }

    public Long category(String name) {
        if (name == null || name.isBlank()) return null;
        var p = Map.of("name", name.trim());
        jdbc.update("INSERT INTO category(category_name) VALUES(:name) ON DUPLICATE KEY UPDATE category_name=category_name", p);
        return jdbc.queryForObject("SELECT category_id FROM category WHERE category_name=:name", p, Long.class);
    }
}
