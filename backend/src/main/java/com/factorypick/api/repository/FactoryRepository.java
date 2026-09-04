package com.factorypick.api.repository;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.dto.FactoryRequest;
import com.factorypick.api.dto.FactorySearchCondition;
import com.factorypick.api.dto.MapMarkerResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class FactoryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public FactoryRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Factory> MAPPER = (rs, rowNum) -> map(rs);

    public List<Factory> search(FactorySearchCondition c) {
        String sql = "SELECT f.* FROM factories f " + where() +
                " ORDER BY f.factory_id DESC LIMIT :limit OFFSET :offset";
        MapSqlParameterSource p = params(c).addValue("limit", c.size()).addValue("offset", c.page() * c.size());
        return jdbc.query(sql, p, MAPPER);
    }

    public long count(FactorySearchCondition c) {
        Long result = jdbc.queryForObject("SELECT COUNT(*) FROM factories f " + where(), params(c), Long.class);
        return result == null ? 0 : result;
    }

    public Optional<Factory> findById(long id) {
        return jdbc.query("SELECT * FROM factories WHERE factory_id=:id", Map.of("id", id), MAPPER).stream().findFirst();
    }

    public Optional<Factory> findByBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.isBlank()) return Optional.empty();
        return jdbc.query("SELECT * FROM factories WHERE business_number=:number",
                Map.of("number", businessNumber), MAPPER).stream().findFirst();
    }

    public Optional<Factory> findExisting(String businessNumber, String factoryName, String address) {
        Optional<Factory> byNumber = findByBusinessNumber(businessNumber);
        if (byNumber.isPresent()) return byNumber;
        return jdbc.query("SELECT * FROM factories WHERE factory_name=:name AND address=:address",
                Map.of("name", factoryName, "address", address), MAPPER).stream().findFirst();
    }

    public List<MapMarkerResponse> markers(double south, double west, double north, double east) {
        String sql = """
                SELECT f.factory_id,f.factory_name,f.company_name,f.latitude,f.longitude,
                       GROUP_CONCAT(DISTINCT p.category ORDER BY p.category SEPARATOR ',') categories
                FROM factories f
                LEFT JOIN factory_products fp ON fp.factory_id=f.factory_id
                LEFT JOIN products p ON p.product_id=fp.product_id
                WHERE f.latitude BETWEEN :south AND :north AND f.longitude BETWEEN :west AND :east
                GROUP BY f.factory_id,f.factory_name,f.company_name,f.latitude,f.longitude
                ORDER BY f.factory_id LIMIT 10000
                """;
        var params = new MapSqlParameterSource("south", south).addValue("west", west)
                .addValue("north", north).addValue("east", east);
        return jdbc.query(sql, params, (rs, n) -> {
            String raw = rs.getString("categories");
            return new MapMarkerResponse(rs.getLong("factory_id"), rs.getString("factory_name"),
                    rs.getString("company_name"), rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"),
                    raw == null || raw.isBlank() ? List.of() : Arrays.asList(raw.split(",")));
        });
    }

    public long insert(FactoryRequest r) {
        String sql = """
                INSERT INTO factories (business_number, factory_name, company_name, address, sido, sigungu,
                  latitude, longitude, industry, established_year, factory_scale, phone)
                VALUES (:businessNumber, :factoryName, :companyName, :address, :sido, :sigungu,
                  :latitude, :longitude, :industry, :establishedYear, :factoryScale, :phone)
                """;
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(sql, values(r), key, new String[]{"factory_id"});
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    public int update(long id, FactoryRequest r) {
        String sql = """
                UPDATE factories SET business_number=:businessNumber, factory_name=:factoryName,
                  company_name=:companyName, address=:address, sido=:sido, sigungu=:sigungu,
                  latitude=:latitude, longitude=:longitude, industry=:industry,
                  established_year=:establishedYear, factory_scale=:factoryScale, phone=:phone
                WHERE factory_id=:id
                """;
        return jdbc.update(sql, values(r).addValue("id", id));
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM factories WHERE factory_id=:id", Map.of("id", id));
    }

    private String where() {
        return """
                 WHERE (:keyword='' OR f.factory_name LIKE CONCAT('%',:keyword,'%')
                    OR f.company_name LIKE CONCAT('%',:keyword,'%') OR f.address LIKE CONCAT('%',:keyword,'%'))
                   AND (:sido='' OR f.sido=:sido)
                   AND (:sigungu='' OR f.sigungu=:sigungu)
                   AND (:product='' OR EXISTS (SELECT 1 FROM factory_products fp JOIN products p ON p.product_id=fp.product_id
                       WHERE fp.factory_id=f.factory_id AND p.product_name LIKE CONCAT('%',:product,'%')))
                   AND (:category='' OR EXISTS (SELECT 1 FROM factory_products fp JOIN products p ON p.product_id=fp.product_id
                       WHERE fp.factory_id=f.factory_id AND p.category=:category))
                """;
    }

    private MapSqlParameterSource params(FactorySearchCondition c) {
        return new MapSqlParameterSource()
                .addValue("keyword", clean(c.keyword())).addValue("sido", clean(c.sido()))
                .addValue("sigungu", clean(c.sigungu())).addValue("product", clean(c.product()))
                .addValue("category", clean(c.category()));
    }

    private MapSqlParameterSource values(FactoryRequest r) {
        return new MapSqlParameterSource()
                .addValue("businessNumber", cleanNull(r.businessNumber())).addValue("factoryName", r.factoryName().trim())
                .addValue("companyName", r.companyName().trim()).addValue("address", r.address().trim())
                .addValue("sido", r.sido().trim()).addValue("sigungu", cleanNull(r.sigungu()))
                .addValue("latitude", r.latitude()).addValue("longitude", r.longitude())
                .addValue("industry", cleanNull(r.industry())).addValue("establishedYear", r.establishedYear())
                .addValue("factoryScale", cleanNull(r.factoryScale())).addValue("phone", cleanNull(r.phone()));
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String cleanNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static Factory map(ResultSet rs) throws SQLException {
        Integer year = (Integer) rs.getObject("established_year");
        return new Factory(rs.getLong("factory_id"), rs.getString("business_number"), rs.getString("factory_name"),
                rs.getString("company_name"), rs.getString("address"), rs.getString("sido"), rs.getString("sigungu"),
                rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"), rs.getString("industry"), year,
                rs.getString("factory_scale"), rs.getString("phone"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }
}
