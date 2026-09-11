package com.factorypick.api.repository;

import com.factorypick.api.domain.*;
import com.factorypick.api.dto.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class ProductRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final RowMapper<Product> MAPPER = (rs, n) -> new Product(rs.getLong("product_id"),
            rs.getString("product_name"), rs.getString("category"), rs.getString("description"),
            rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());

    private final ReferenceRepository references;
    private static final String FROM = " FROM product p LEFT JOIN category c ON c.category_id=p.category_id ";
    private static final String SELECT = "SELECT p.*, c.category_name AS category" + FROM;
    public ProductRepository(NamedParameterJdbcTemplate jdbc, ReferenceRepository references) {
        this.jdbc = jdbc; this.references = references;
    }

    public List<Product> search(String keyword, String category, int page, int size) {
        var p = params(keyword, category).addValue("limit", size).addValue("offset", page * size);
        return jdbc.query(SELECT + where() + " ORDER BY p.product_name LIMIT :limit OFFSET :offset", p, MAPPER);
    }

    public long count(String keyword, String category) {
        Long n = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where(), params(keyword, category), Long.class);
        return n == null ? 0 : n;
    }

    public Optional<Product> findById(long id) {
        return jdbc.query(SELECT + " WHERE p.product_id=:id", Map.of("id", id), MAPPER).stream().findFirst();
    }

    public Optional<Product> findByNameAndCategory(String name, String category) {
        return jdbc.query(SELECT + " WHERE p.product_name=:name AND c.category_name <=> :category",
                new MapSqlParameterSource("name", name).addValue("category", category == null || category.isBlank() ? null : category.trim()), MAPPER).stream().findFirst();
    }

    public List<Product> findByFactoryId(long factoryId) {
        return jdbc.query(SELECT + """
                JOIN factory_product fp ON fp.product_id=p.product_id
                WHERE fp.factory_id=:factoryId ORDER BY p.product_name
                """, Map.of("factoryId", factoryId), MAPPER);
    }

    @Transactional
    public long insert(ProductRequest r) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO product(product_name,category_id,description) VALUES(:name,:categoryId,:description)",
                values(r), key, new String[]{"product_id"});
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    @Transactional
    public int update(long id, ProductRequest r) {
        return jdbc.update("UPDATE product SET product_name=:name,category_id=:categoryId,description=:description WHERE product_id=:id",
                values(r).addValue("id", id));
    }

    public int delete(long id) { return jdbc.update("DELETE FROM product WHERE product_id=:id", Map.of("id", id)); }

    public void replaceFactoryProducts(long factoryId, List<Long> productIds) {
        jdbc.update("DELETE FROM factory_product WHERE factory_id=:id", Map.of("id", factoryId));
        for (Long productId : new LinkedHashSet<>(productIds)) {
            jdbc.update("INSERT INTO factory_product(factory_id,product_id) VALUES(:factoryId,:productId)",
                    Map.of("factoryId", factoryId, "productId", productId));
        }
    }

    public void linkFactoryProduct(long factoryId, long productId) {
        jdbc.update("INSERT IGNORE INTO factory_product(factory_id,product_id) VALUES(:factoryId,:productId)",
                Map.of("factoryId", factoryId, "productId", productId));
    }

    public List<Factory> findFactoriesByProduct(long productId) {
        String sql = FactoryRepository.SELECT + """
                JOIN factory_product fp ON fp.factory_id=f.factory_id
                WHERE fp.product_id=:id ORDER BY f.factory_name
                """;
        return jdbc.query(sql, Map.of("id", productId), (rs, n) -> FactoryRepository.map(rs));
    }

    public List<String> categories() {
        return jdbc.queryForList("SELECT DISTINCT c.category_name" + FROM + " WHERE c.category_name IS NOT NULL ORDER BY c.category_name", Map.of(), String.class);
    }

    private String where() {
        return "WHERE (:keyword='' OR p.product_name LIKE CONCAT('%',:keyword,'%')) AND (:category='' OR c.category_name=:category)";
    }
    private MapSqlParameterSource params(String keyword, String category) {
        return new MapSqlParameterSource("keyword", clean(keyword)).addValue("category", clean(category));
    }
    private MapSqlParameterSource values(ProductRequest r) {
        return new MapSqlParameterSource("name", r.productName().trim()).addValue("categoryId", references.category(r.category()))
                .addValue("description", r.description() == null || r.description().isBlank() ? null : r.description().trim());
    }
    private String clean(String s) { return s == null ? "" : s.trim(); }
}
