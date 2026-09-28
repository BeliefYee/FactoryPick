package com.factorypick.api;

import com.factorypick.api.dto.*;
import com.factorypick.api.repository.*;
import com.factorypick.api.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in: MYSQL_TEST_URL must point to a separate test database. */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${MYSQL_TEST_URL}",
    "spring.datasource.username=${MYSQL_TEST_USERNAME:root}",
    "spring.datasource.password=${MYSQL_TEST_PASSWORD:1234}",
    "spring.sql.init.mode=always"
})
@Transactional
class NormalizedDatabaseTest {
    @Autowired FactoryService factories;
    @Autowired ProductService products;
    @Autowired FactoryRepository factoryRepository;
    @Autowired StatisticsRepository statistics;
    @Autowired DataImportService imports;
    @Autowired JdbcTemplate jdbc;

    @Test void geocodingPersistsOnlyForTheAddressThatWasLookedUp() {
        var f = factories.create(new FactoryRequest(null, "geocode", null, "original address", null, null,
                null, null, null, null, null, null), List.of()).factory();
        var result = new com.factorypick.api.service.KakaoGeocodingClient.Result("RESOLVED",
                new BigDecimal("37.5"), new BigDecimal("127.1"));
        assertThat(factoryRepository.saveGeocoding(f.factoryId(), "old address", result)).isZero();
        assertThat(factoryRepository.saveGeocoding(f.factoryId(), "original address", result)).isEqualTo(1);
        var saved = factories.detail(f.factoryId()).factory();
        assertThat(saved.geocodingStatus()).isEqualTo("RESOLVED");
        assertThat(saved.geocodedAt()).isNotNull();
        assertThat(saved.latitude()).isEqualByComparingTo("37.5");
        assertThat(factoryRepository.saveGeocoding(f.factoryId(), "original address", result)).isZero();
    }

    @Test void publicDataWithoutCoordinatesPreservesMetadataAndSupportsUnclassifiedProducts() {
        var request = new FactoryRequest(null, "삼성전자(주) 온양사업장", null,
            "충청남도 아산시 배방읍 배방로 158", null, null, null, null, null, null, null, null);
        var factory = factories.create(request, List.of()).factory();
        jdbc.update("""
            UPDATE factory SET factory_manage_no=?, representative_name=?, managing_agency_name=?,
            fax_number=?, employee_count=?, first_registered_date=?, primary_industry_code=?,
            main_product_text=?, homepage_raw=?, industrial_complex_name=?, source_payload=?, last_synced_at=NOW()
            WHERE factory_id=?
            """, "130111000624600", "전영현", "충청남도 아산시", "041-540-6030", 5705,
            "1994-11-09", "26111", "반도체 pkg, 반도체 pkg", "79-2", "남동국가산업단지",
            "{\"fctryManageNo\":\"130111000624600\",\"hmpadr\":\"79-2\"}", factory.factoryId());
        jdbc.update("INSERT INTO factory_industry(factory_id,industry_code) VALUES (?, '26111'), (?, '26112')",
            factory.factoryId(), factory.factoryId());
        var loaded = factoryRepository.findByManageNo("130111000624600").orElseThrow();
        assertThat(loaded.employeeCount()).isEqualTo(5705);
        assertThat(loaded.firstRegisteredDate()).isEqualTo(java.time.LocalDate.of(1994, 11, 9));
        assertThat(loaded.establishedYear()).isNull();
        assertThat(loaded.homepageRaw()).isEqualTo("79-2");
        assertThat(loaded.geocodingStatus()).isEqualTo("PENDING");
        assertThat(loaded.latitude()).isNull();
        assertThat(factoryRepository.markers(33, 124, 39, 132)).noneMatch(m -> m.factoryId() == factory.factoryId());
        assertThat(factories.search("온양", null, null, null, null, 0, 20).totalElements()).isEqualTo(1);
        var product = products.create(new ProductRequest("반도체 pkg", null, null));
        assertThat(product.category()).isNull();
        factories.update(factory.factoryId(), request, List.of(product.productId()));
        assertThat(statistics.byRegion()).contains(new StatisticsResponse("미분류", 1));
        assertThat(statistics.byCategory()).contains(new StatisticsResponse("전자·반도체", 1));
        assertThat(products.search("반도체", null, 0, 20).totalElements()).isEqualTo(1);
        factories.delete(factory.factoryId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factory_industry WHERE factory_id=?", Long.class, factory.factoryId())).isZero();
    }

    @Test void industryCategoriesWorkWithoutProductsAndStayInSyncWithExistingProductCategories() {
        var factory = factories.create(request("INDUSTRY-1", "metal works", null, null, null), List.of()).factory();
        factoryRepository.savePrimaryIndustryCode(factory.factoryId(), "25999");
        assertThat(factoryRepository.categories()).contains("금속·철강").doesNotContain("통신");
        assertThat(factories.detail(factory.factoryId()).categories()).containsExactly("금속·철강");
        assertThat(factories.search(null,null,null,null,"금속·철강",0,20).content())
                .extracting(com.factorypick.api.domain.Factory::factoryId).containsExactly(factory.factoryId());
        assertThat(factoryRepository.markers(37,127,38,128,
                new FactorySearchCondition(null,null,null,null,"금속·철강",0,20)))
                .singleElement().satisfies(m -> assertThat(m.categories()).containsExactly("금속·철강"));
        assertThat(statistics.byCategory()).contains(new StatisticsResponse("금속·철강",1));

        var product = products.create(new ProductRequest("metal part", "금속·철강", null));
        var second = products.create(new ProductRequest("special part", "custom category", null));
        factories.update(factory.factoryId(), request("INDUSTRY-1","metal works",null,null,null),
                List.of(product.productId(),second.productId()));
        assertThat(factories.detail(factory.factoryId()).categories()).containsExactlyInAnyOrder("금속·철강","custom category");
        assertThat(statistics.byCategory()).contains(new StatisticsResponse("금속·철강",1));
        assertThat(products.search(null,"금속·철강",0,20).content()).hasSize(2);
        factoryRepository.savePrimaryIndustryCode(factory.factoryId(), " ");
        assertThat(factories.detail(factory.factoryId()).factory().primaryIndustryCode()).isEqualTo("25999");

        factoryRepository.savePrimaryIndustryCode(factory.factoryId(), "58222");
        assertThat(factories.detail(factory.factoryId()).categories()).contains("소프트웨어·정보서비스");
        factoryRepository.savePrimaryIndustryCode(factory.factoryId(), "58111");
        assertThat(factories.detail(factory.factoryId()).categories()).contains("인쇄·출판").doesNotContain("소프트웨어·정보서비스");
    }

    @Test void mapReturnsAllFactoriesInTheViewportForClustering() {
        jdbc.batchUpdate("INSERT INTO factory(factory_name,address,latitude,longitude) VALUES (?,'서울특별시 금천구 디지털로9길 33, 502호',37.5,127.5)",
                java.util.stream.IntStream.range(0,10001)
                        .mapToObj(i -> new Object[]{"cluster-test-" + i}).toList());
        var markers = factoryRepository.markers(37,127,38,128,
                new FactorySearchCondition("cluster-test-",null,null,null,null,0,20));
        assertThat(markers).hasSize(10001);
        assertThat(markers).extracting(MapMarkerResponse::factoryId).doesNotHaveDuplicates();
        assertThat(markers).allSatisfy(marker -> assertThat(marker.address()).isEqualTo("서울특별시 금천구 디지털로9길 33, 502호"));
        assertThat(factoryRepository.markers(33,124,34,125,
                new FactorySearchCondition("cluster-test-",null,null,null,null,0,20))).isEmpty();
    }

    @Test void databaseRejectsDuplicateManageNumbersAndPartialCoordinates() {
        jdbc.update("INSERT INTO factory(factory_name,factory_manage_no) VALUES ('same','DUP-1')");
        jdbc.update("INSERT INTO factory(factory_name,factory_manage_no) VALUES ('same','DUP-2')");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO factory(factory_name,factory_manage_no) VALUES ('other','DUP-1')"))
            .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO factory(factory_name,latitude) VALUES ('partial',37.5)"))
            .isInstanceOf(org.springframework.dao.DataAccessException.class)
            .hasMessageContaining("chk_factory_coordinate_pair");
        jdbc.update("INSERT INTO product(product_name) VALUES ('unclassified')");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO product(product_name) VALUES ('unclassified')"))
            .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    private FactoryRequest request(String number, String name, String company, String sido, String sigungu) {
        return new FactoryRequest(number, name, company, "test address " + number, sido, sigungu,
            new BigDecimal("37.5"), new BigDecimal("127.1"), "food", 2015, "small", "02-000-0000");
    }

    @Test void csvAcceptsMissingCoordinatesAndCategory() {
        String csv = "factory_name,product_name\nUnknown location,Unclassified product\n";
        var result = imports.importCsv(new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)));
        assertThat(result.failedRows()).isZero();
        assertThat(result.insertedRows()).isEqualTo(1);
        var factory = factories.search("Unknown location", null, null, null, null, 0, 20).content().get(0);
        assertThat(factory.latitude()).isNull();
        assertThat(factory.companyName()).isNull();
        assertThat(factories.detail(factory.factoryId()).products()).hasSize(1);
    }

    @Test void joinsFiltersUpdatesAndCascadesPreserveTheApiContract() {
        var product = products.create(new ProductRequest("test milk", "test dairy", "description"));
        var first = factories.create(request("TEST-1", "first", "shared company", "test sido", null), List.of(product.productId())).factory();
        var second = factories.create(request("TEST-2", "second", "shared company", "test sido", ""), List.of(product.productId())).factory();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM company WHERE company_name='shared company'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM region WHERE sido_name='test sido'", Long.class)).isEqualTo(1);
        assertThat(first.sigungu()).isNull();
        assertThat(first.establishedYear()).isEqualTo(2015);
        assertThat(first.industry()).isEqualTo("food");
        assertThat(factories.search("shared company", "test sido", null, "milk", "test dairy", 0, 1).totalElements()).isEqualTo(2);
        assertThat(factories.search(null, null, null, null, "missing", 0, 20).content()).isEmpty();
        assertThat(products.search("milk", "test dairy", 0, 20).totalElements()).isEqualTo(1);
        assertThat(products.factories(product.productId())).hasSize(2).allSatisfy(f -> assertThat(f.latitude()).isEqualByComparingTo("37.5"));
        assertThat(factoryRepository.markers(37, 127, 38, 128)).anySatisfy(m -> assertThat(m.categories()).contains("test dairy"));
        assertThat(factoryRepository.markers(37,127,38,128,
                new FactorySearchCondition("first", "test sido", null, "milk", "test dairy",0,20)))
                .extracting(MapMarkerResponse::factoryId).containsExactly(first.factoryId());
        assertThat(factoryRepository.markers(37,127,38,128,
                new FactorySearchCondition(null,null,null,null,"missing",0,20))).isEmpty();
        assertThat(factoryRepository.markers(33,124,34,125,
                new FactorySearchCondition(null,null,null,null,"test dairy",0,20))).isEmpty();
        assertThat(statistics.byRegion()).contains(new StatisticsResponse("test sido", 2));
        assertThat(statistics.byCategory()).contains(new StatisticsResponse("test dairy", 2));
        assertThat(statistics.byProduct()).contains(new StatisticsResponse("test milk", 2));

        factories.update(first.factoryId(), request("TEST-1", "renamed", "new company", "new sido", "new district"), null);
        assertThat(factories.detail(first.factoryId()).products()).hasSize(1);
        assertThat(factories.detail(second.factoryId()).factory().companyName()).isEqualTo("shared company");
        assertThat(factories.search(null, "new sido", "new district", null, null, 0, 20).totalElements()).isEqualTo(1);
        products.update(product.productId(), new ProductRequest("test milk", "new category", "updated"));
        assertThat(products.categories()).contains("new category").doesNotContain("test dairy");
        assertThat(factories.detail(first.factoryId()).products().get(0).category()).isEqualTo("new category");
        factories.delete(first.factoryId());
        assertThat(products.factories(product.productId())).hasSize(1);
        products.delete(product.productId());
        assertThat(factories.detail(second.factoryId()).products()).isEmpty();
    }

    @Test void csvImportReusesReferencesAndUpdatesExistingFactories() {
        String csv = "business_number,factory_name,company_name,address,sido,sigungu,latitude,longitude,product_name,category\n"
            + "CSV-1,CSV factory,CSV company,CSV address,CSV sido,,37.5,127.1,CSV product,CSV category\n";
        var file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        assertThat(imports.importCsv(file).insertedRows()).isEqualTo(1);
        assertThat(imports.importCsv(file).updatedRows()).isEqualTo(1);
        var factory = factoryRepository.findExisting(null, "CSV factory", "CSV address").orElseThrow();
        assertThat(factories.detail(factory.factoryId()).products()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM company WHERE company_name='CSV company'", Long.class)).isEqualTo(1);
        assertThat(products.search("CSV product", "CSV category", 0, 20).totalElements()).isEqualTo(1);
    }
}
