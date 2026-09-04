package com.factorypick.api.repository;

import com.factorypick.api.dto.StatisticsResponse;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class StatisticsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public StatisticsRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<StatisticsResponse> byRegion() {
        return query("SELECT sido label, COUNT(*) count FROM factories GROUP BY sido ORDER BY count DESC");
    }
    public List<StatisticsResponse> byCategory() {
        return query("""
                SELECT p.category label, COUNT(DISTINCT fp.factory_id) count FROM products p
                JOIN factory_products fp ON fp.product_id=p.product_id GROUP BY p.category ORDER BY count DESC
                """);
    }
    public List<StatisticsResponse> byProduct() {
        return query("""
                SELECT p.product_name label, COUNT(DISTINCT fp.factory_id) count FROM products p
                LEFT JOIN factory_products fp ON fp.product_id=p.product_id GROUP BY p.product_id,p.product_name ORDER BY count DESC
                """);
    }
    private List<StatisticsResponse> query(String sql) {
        return jdbc.query(sql, Map.of(), (rs, n) -> new StatisticsResponse(rs.getString("label"), rs.getLong("count")));
    }
}
