package com.factorypick.api.repository;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.dto.FactoryRequest;
import com.factorypick.api.dto.FactorySearchCondition;
import com.factorypick.api.dto.MapMarkerResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class FactoryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    private final ReferenceRepository references;
    public FactoryRepository(NamedParameterJdbcTemplate jdbc, ReferenceRepository references) {
        this.jdbc = jdbc; this.references = references;
    }
    static final String FROM = " FROM factory f LEFT JOIN company c ON c.company_id=f.company_id LEFT JOIN region r ON r.region_id=f.region_id ";
    static final String SELECT = "SELECT f.*, c.company_name, r.sido_name AS sido, NULLIF(r.sigungu_name,'') AS sigungu, f.industry_name AS industry" + FROM;

    private static final RowMapper<Factory> MAPPER = (rs, rowNum) -> map(rs);

    public List<Factory> search(FactorySearchCondition c) {
        String sql = SELECT + where() +
                " ORDER BY f.factory_id DESC LIMIT :limit OFFSET :offset";
        MapSqlParameterSource p = params(c).addValue("limit", c.size()).addValue("offset", c.page() * c.size());
        return jdbc.query(sql, p, MAPPER);
    }

    public long count(FactorySearchCondition c) {
        Long result = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where(), params(c), Long.class);
        return result == null ? 0 : result;
    }

    public Optional<Factory> findById(long id) {
        return jdbc.query(SELECT + " WHERE f.factory_id=:id", Map.of("id", id), MAPPER).stream().findFirst();
    }

    public Optional<Factory> findByBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.isBlank()) return Optional.empty();
        return jdbc.query(SELECT + " WHERE f.business_number=:number",
                Map.of("number", businessNumber), MAPPER).stream().findFirst();
    }

    public Optional<Factory> findExisting(String businessNumber, String factoryName, String address) {
        Optional<Factory> byNumber = findByBusinessNumber(businessNumber);
        if (byNumber.isPresent()) return byNumber;
        return jdbc.query(SELECT + " WHERE f.factory_name=:name AND f.address=:address",
                new MapSqlParameterSource("name", factoryName).addValue("address", address), MAPPER).stream().findFirst();
    }

    public Optional<Factory> findByManageNo(String manageNo) {
        if (manageNo == null || manageNo.isBlank()) return Optional.empty();
        return jdbc.query(SELECT + " WHERE f.factory_manage_no=:number",
                Map.of("number", manageNo.trim()), MAPPER).stream().findFirst();
    }

    public List<MapMarkerResponse> markers(double south, double west, double north, double east) {
        return markers(south, west, north, east, new FactorySearchCondition(null,null,null,null,0,20));
    }

    public List<MapMarkerResponse> markers(double south, double west, double north, double east, FactorySearchCondition condition) {
        String sql = """
                SELECT f.factory_id,f.factory_name,c.company_name,f.address,f.latitude,f.longitude,
                       GROUP_CONCAT(DISTINCT cat.category_name ORDER BY cat.category_name SEPARATOR ',') categories
                FROM factory f
                LEFT JOIN company c ON c.company_id=f.company_id
                LEFT JOIN region r ON r.region_id=f.region_id
                LEFT JOIN factory_category fc ON fc.factory_id=f.factory_id
                LEFT JOIN category cat ON cat.category_id=fc.category_id
                """ + where() + """
                AND f.latitude BETWEEN :south AND :north AND f.longitude BETWEEN :west AND :east
                GROUP BY f.factory_id,f.factory_name,c.company_name,f.address,f.latitude,f.longitude
                ORDER BY f.factory_id
                """;
        var params = params(condition).addValue("south", south).addValue("west", west)
                .addValue("north", north).addValue("east", east);
        return jdbc.query(sql, params, (rs, n) -> {
            String raw = rs.getString("categories");
            return new MapMarkerResponse(rs.getLong("factory_id"), rs.getString("factory_name"),
                    rs.getString("company_name"), rs.getString("address"), rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"),
                    raw == null || raw.isBlank() ? List.of() : Arrays.asList(raw.split(",")));
        });
    }

    @Transactional
    public long insert(FactoryRequest r) {
        String sql = """
                INSERT INTO factory (business_number, factory_name, company_id, address, region_id,
                  latitude, longitude, industry_name, established_year, factory_scale, phone, geocoding_status)
                VALUES (:businessNumber, :factoryName, :companyId, :address, :regionId,
                  :latitude, :longitude, :industry, :establishedYear, :factoryScale, :phone, :geocodingStatus)
                """;
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(sql, values(r), key, new String[]{"factory_id"});
        return Objects.requireNonNull(key.getKey()).longValue();
    }
public int saveFactoryManageNo(long factoryId, String factoryManageNo) {
    return jdbc.update("""
        UPDATE factory
        SET factory_manage_no = :factoryManageNo
        WHERE factory_id = :factoryId
        """,
        new MapSqlParameterSource()
            .addValue("factoryId", factoryId)
            .addValue("factoryManageNo",
                    factoryManageNo == null || factoryManageNo.isBlank()
                            ? null
                            : factoryManageNo.trim())
    );
}

    public int saveIndustrialComplexName(long factoryId, String industrialComplexName) {
        return jdbc.update("""
            UPDATE factory
            SET industrial_complex_name = :industrialComplexName
            WHERE factory_id = :factoryId
            """,
            new MapSqlParameterSource()
                .addValue("factoryId", factoryId)
                .addValue("industrialComplexName", industrialComplexName)
        );
    }

    @Transactional
    public int update(long id, FactoryRequest r) {
        String sql = """
                UPDATE factory SET business_number=:businessNumber, factory_name=:factoryName,
                  company_id=:companyId, address=:address, region_id=:regionId,
                  latitude=:latitude, longitude=:longitude, industry_name=:industry,
                  established_year=:establishedYear, factory_scale=:factoryScale, phone=:phone,
                  geocoding_status=:geocodingStatus, geocoded_at=NULL
                WHERE factory_id=:id
                """;
        return jdbc.update(sql, values(r).addValue("id", id));
    }

    public List<String> categories() {
        return jdbc.queryForList("""
                SELECT DISTINCT c.category_name FROM factory_category fc
                JOIN category c ON c.category_id=fc.category_id ORDER BY c.category_name
                """, Map.of(), String.class);
    }

    public List<String> categoriesForFactory(long id) {
        return jdbc.queryForList("""
                SELECT c.category_name FROM factory_category fc
                JOIN category c ON c.category_id=fc.category_id
                WHERE fc.factory_id=:id ORDER BY c.category_name
                """, Map.of("id", id), String.class);
    }

    public int savePrimaryIndustryCode(long id, String code) {
        return jdbc.update("""
                UPDATE factory SET primary_industry_code=COALESCE(:code,primary_industry_code)
                WHERE factory_id=:id
                """, new MapSqlParameterSource("id", id).addValue("code", cleanNull(code)));}
    @Transactional
    public int updateFromPublicApi(long id, FactoryRequest r) {
        String sql = """
                UPDATE factory SET factory_name=:factoryName, company_id=:companyId,
                  latitude=CASE WHEN address <=> :address THEN latitude ELSE NULL END,
                  longitude=CASE WHEN address <=> :address THEN longitude ELSE NULL END,
                  geocoding_status=CASE WHEN address <=> :address THEN geocoding_status ELSE 'PENDING' END,
                  geocoded_at=CASE WHEN address <=> :address THEN geocoded_at ELSE NULL END,
                  address=:address, region_id=:regionId, industry_name=:industry,
                  established_year=:establishedYear, phone=:phone
                WHERE factory_id=:id
                """;
        return jdbc.update(sql, values(r).addValue("id", id));
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM factory WHERE factory_id=:id", Map.of("id", id));
    }

    public int saveGeocoding(long id, String originalAddress,
            com.factorypick.api.service.KakaoGeocodingClient.Result result) {
        return jdbc.update("""
            UPDATE factory SET latitude=:latitude, longitude=:longitude, geocoding_status=:status,
              geocoded_at=CASE WHEN :status='RESOLVED' THEN NOW() ELSE NULL END
            WHERE factory_id=:id AND address <=> :address AND latitude IS NULL AND longitude IS NULL
            """, new MapSqlParameterSource("id", id).addValue("address", originalAddress)
                .addValue("latitude", result.latitude()).addValue("longitude", result.longitude())
                .addValue("status", result.status()));
    }

    private String where() {
        return """
                 WHERE (:keyword='' OR f.factory_name LIKE CONCAT('%',:keyword,'%')
                    OR c.company_name LIKE CONCAT('%',:keyword,'%') OR f.address LIKE CONCAT('%',:keyword,'%'))
                   AND (:sido='' OR r.sido_name=:sido)
                   AND (:sigungu='' OR r.sigungu_name IN
                       (:sigungu, CONCAT(:sigungu,'시'), CONCAT(:sigungu,'군'), CONCAT(:sigungu,'구')))
                   AND (:category='' OR EXISTS (SELECT 1 FROM factory_category fc
                       JOIN category cat ON cat.category_id=fc.category_id
                       WHERE fc.factory_id=f.factory_id AND cat.category_name=:category))
                """;
    }

    private MapSqlParameterSource params(FactorySearchCondition c) {
        return new MapSqlParameterSource()
                .addValue("keyword", clean(c.keyword())).addValue("sido", clean(c.sido()))
                .addValue("sigungu", clean(c.sigungu()))
                .addValue("category", clean(c.category()));
    }

    private MapSqlParameterSource values(FactoryRequest r) {
        if (!r.isCoordinatePairValid()) throw new IllegalArgumentException("위도와 경도는 함께 입력해야 합니다.");
        return new MapSqlParameterSource()
                .addValue("businessNumber", cleanNull(r.businessNumber())).addValue("factoryName", r.factoryName().trim())
                .addValue("companyId", references.company(r.companyName())).addValue("address", cleanNull(r.address()))
                .addValue("regionId", references.region(r.sido(), r.sigungu()))
                .addValue("latitude", r.latitude()).addValue("longitude", r.longitude())
                .addValue("industry", cleanNull(r.industry())).addValue("establishedYear", r.establishedYear())
                .addValue("factoryScale", cleanNull(r.factoryScale())).addValue("phone", cleanNull(r.phone()))
                .addValue("geocodingStatus", r.latitude() == null ? "PENDING" : "RESOLVED");
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String cleanNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    static Factory map(ResultSet rs) throws SQLException {
        Integer year = rs.getObject("established_year", Integer.class);
        return new Factory(rs.getLong("factory_id"), rs.getString("business_number"), rs.getString("factory_name"),
                rs.getString("company_name"), rs.getString("address"), rs.getString("sido"), rs.getString("sigungu"),
                rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"), rs.getString("industry"), year,
                rs.getString("factory_scale"), rs.getString("phone"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getString("factory_manage_no"), rs.getString("representative_name"),
                rs.getString("managing_agency_name"), rs.getString("fax_number"),
                rs.getObject("employee_count", Integer.class), rs.getObject("first_registered_date", java.time.LocalDate.class),
                rs.getString("primary_industry_code"), rs.getString("main_product_text"), rs.getString("homepage_raw"),
                rs.getString("industrial_complex_name"), rs.getString("geocoding_status"),
                rs.getObject("geocoded_at", java.time.LocalDateTime.class), rs.getObject("last_synced_at", java.time.LocalDateTime.class));
    }
}
