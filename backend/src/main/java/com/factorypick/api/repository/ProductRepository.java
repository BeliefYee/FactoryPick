package com.factorypick.api.repository;

import com.factorypick.api.domain.*;
import com.factorypick.api.dto.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ProductRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final RowMapper<Product> MAPPER = (rs, n) -> new Product(rs.getLong("product_id"),
            rs.getString("product_name"), rs.getString("category"), rs.getString("description"),
            rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());

    public ProductRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Product> search(String keyword, String category, int page, int size) {
        var p = params(keyword, category).addValue("limit", size).addValue("offset", page * size);
        return jdbc.query("SELECT * FROM products " + where() + " ORDER BY product_name LIMIT :limit OFFSET :offset", p, MAPPER);
    }

    public long count(String keyword, String category) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM products " + where(), params(keyword, category), Long.class);
        return n == null ? 0 : n;
    }

    public Optional<Product> findById(long id) {
        return jdbc.query("SELECT * FROM products WHERE product_id=:id", Map.of("id", id), MAPPER).stream().findFirst();
    }

    public Optional<Product> findByNameAndCategory(String name, String category) {
        return jdbc.query("SELECT * FROM products WHERE product_name=:name AND category=:category",
                Map.of("name", name, "category", category), MAPPER).stream().findFirst();
    }

    public List<Product> findByFactoryId(long factoryId) {
        return jdbc.query("""
                SELECT p.* FROM products p JOIN factory_products fp ON fp.product_id=p.product_id
                WHERE fp.factory_id=:factoryId ORDER BY p.product_name
                """, Map.of("factoryId", factoryId), MAPPER);
    }

    public long insert(ProductRequest r) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO products(product_name,category,description) VALUES(:name,:category,:description)",
                values(r), key, new String[]{"product_id"});
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    public int update(long id, ProductRequest r) {
        return jdbc.update("UPDATE products SET product_name=:name,category=:category,description=:description WHERE product_id=:id",
                values(r).addValue("id", id));
    }

    public int delete(long id) { return jdbc.update("DELETE FROM products WHERE product_id=:id", Map.of("id", id)); }

    public void replaceFactoryProducts(long factoryId, List<Long> productIds) {
        jdbc.update("DELETE FROM factory_products WHERE factory_id=:id", Map.of("id", factoryId));
        for (Long productId : new LinkedHashSet<>(productIds)) {
            jdbc.update("INSERT INTO factory_products(factory_id,product_id) VALUES(:factoryId,:productId)",
                    Map.of("factoryId", factoryId, "productId", productId));
        }
    }

    public void linkFactoryProduct(long factoryId, long productId) {
        jdbc.update("INSERT IGNORE INTO factory_products(factory_id,product_id) VALUES(:factoryId,:productId)",
                Map.of("factoryId", factoryId, "productId", productId));
    }

    public List<Factory> findFactoriesByProduct(long productId) {
        String sql = """
                SELECT f.* FROM factories f JOIN factory_products fp ON fp.factory_id=f.factory_id
                WHERE fp.product_id=:id ORDER BY f.factory_name
                """;
        return jdbc.query(sql, Map.of("id", productId), (rs, n) -> new Factory(rs.getLong("factory_id"),
                rs.getString("business_number"), rs.getString("factory_name"), rs.getString("company_name"),
                rs.getString("address"), rs.getString("sido"), rs.getString("sigungu"), rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"), rs.getString("industry"), (Integer) rs.getObject("established_year"),
                rs.getString("factory_scale"), rs.getString("phone"), rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()));
    }

    public List<String> categories() {
        return jdbc.queryForList("SELECT DISTINCT category FROM products ORDER BY category", Map.of(), String.class);
    }

    private String where() {
        return "WHERE (:keyword='' OR product_name LIKE CONCAT('%',:keyword,'%')) AND (:category='' OR category=:category)";
    }
    private MapSqlParameterSource params(String keyword, String category) {
        return new MapSqlParameterSource("keyword", clean(keyword)).addValue("category", clean(category));
    }
    private MapSqlParameterSource values(ProductRequest r) {
        return new MapSqlParameterSource("name", r.productName().trim()).addValue("category", r.category().trim())
                .addValue("description", r.description() == null || r.description().isBlank() ? null : r.description().trim());
    }
    private String clean(String s) { return s == null ? "" : s.trim(); }
}
