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
        return query("SELECT COALESCE(r.sido_name,'미분류') label, COUNT(*) count FROM factory f LEFT JOIN region r ON r.region_id=f.region_id GROUP BY r.sido_name ORDER BY count DESC");
    }
    public List<StatisticsResponse> byCategory() {
        return query("""
                SELECT COALESCE(c.category_name,'미분류') label, COUNT(DISTINCT f.factory_id) count FROM factory f
                LEFT JOIN factory_category fc ON fc.factory_id=f.factory_id
                LEFT JOIN category c ON c.category_id=fc.category_id
                GROUP BY c.category_id,c.category_name ORDER BY count DESC, label
                """);
    }
    public List<StatisticsResponse> byProduct() {
        return query("""
                SELECT p.product_name label, COUNT(DISTINCT fp.factory_id) count FROM product p
                LEFT JOIN factory_product fp ON fp.product_id=p.product_id GROUP BY p.product_id,p.product_name ORDER BY count DESC
                """);
    }
    private List<StatisticsResponse> query(String sql) {
        return jdbc.query(sql, Map.of(), (rs, n) -> new StatisticsResponse(rs.getString("label"), rs.getLong("count")));
    }
}
