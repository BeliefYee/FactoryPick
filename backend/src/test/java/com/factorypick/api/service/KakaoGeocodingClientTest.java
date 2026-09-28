package com.factorypick.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KakaoGeocodingClientTest {
    private KakaoGeocodingClient client(String json) {
        var builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://dapi.kakao.com/v2/local/search/address.json?")))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(queryParam("analyze_type", "exact"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        return new KakaoGeocodingClient(builder.build(), "test-key");
    }

    @Test void mapsLongitudeAndLatitudeCorrectly() {
        var result = client("""
            {"meta":{"total_count":1},"documents":[{"address_type":"ROAD_ADDR","x":"127.1","y":"37.5"}]}
            """).lookup("서울특별시 송파구 올림픽로 300");
        assertThat(result.status()).isEqualTo("RESOLVED");
        assertThat(result.latitude()).isEqualByComparingTo("37.5");
        assertThat(result.longitude()).isEqualByComparingTo("127.1");
    }
    @Test void emptyAndAmbiguousResultsAreNotSavedAsCoordinates() {
        assertThat(client("{\"meta\":{\"total_count\":0},\"documents\":[]}").lookup("address").status()).isEqualTo("NOT_FOUND");
        assertThat(client("{\"meta\":{\"total_count\":2},\"documents\":[{},{}]}").lookup("address").status()).isEqualTo("REVIEW");
        assertThat(client("{\"meta\":{\"total_count\":1},\"documents\":[{\"address_type\":\"REGION\"}]}").lookup("address").status()).isEqualTo("REVIEW");
    }
    @Test void malformedOrUnauthorizedResponsesDoNotLeakCredentials() {
        assertThat(client("{}").lookup("address").status()).isEqualTo("FAILED");
        var builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withUnauthorizedRequest());
        assertThatThrownBy(() -> new KakaoGeocodingClient(builder.build(), "secret").lookup("address"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503").hasMessageNotContaining("secret");
    }

    @Test void retriesOnlyTheCompleteRoadAddressAfterNoResults() {
        var builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(queryParam("query", org.springframework.web.util.UriUtils.encode(
                        "경기도 시흥시 공단1대로379번길 31,시화단지 4나 102호 (정왕동)", java.nio.charset.StandardCharsets.UTF_8)))
                .andRespond(withSuccess("{\"meta\":{\"total_count\":0},\"documents\":[]}", MediaType.APPLICATION_JSON));
        server.expect(queryParam("query", org.springframework.web.util.UriUtils.encode(
                        "경기도 시흥시 공단1대로379번길 31", java.nio.charset.StandardCharsets.UTF_8)))
                .andExpect(queryParam("analyze_type", "exact"))
                .andRespond(withSuccess("""
                    {"meta":{"total_count":1},"documents":[{"address_type":"ROAD_ADDR","x":"126.74","y":"37.33"}]}
                    """, MediaType.APPLICATION_JSON));
        assertThat(new KakaoGeocodingClient(builder.build(), "test-key")
                .lookup("경기도 시흥시 공단1대로379번길 31,시화단지 4나 102호 (정왕동)").status()).isEqualTo("RESOLVED");
        server.verify();
    }

    @Test void addressCleanupDoesNotGuessMissingBuildingNumbersOrLotAddresses() {
        assertThat(KakaoGeocodingClient.roadAddress("서울특별시 송파구 올림픽로 (잠실동)"))
                .isEqualTo("서울특별시 송파구 올림픽로 (잠실동)");
        assertThat(KakaoGeocodingClient.roadAddress("경기도 시흥시 정왕동 123-4 (공장)"))
                .isEqualTo("경기도 시흥시 정왕동 123-4 (공장)");
        assertThat(KakaoGeocodingClient.roadAddress("서울특별시 송파구 올림픽로 300 (신천동)"))
                .isEqualTo("서울특별시 송파구 올림픽로 300");
    }

    @Test void ambiguousOriginalAddressIsNotRetriedWithLessDetail() {
        assertThat(client("{\"meta\":{\"total_count\":2},\"documents\":[{},{}]}")
                .lookup("서울특별시 송파구 올림픽로 300, 2층").status()).isEqualTo("REVIEW");
    }

    @Test void providerRateLimitStopsRetriesWithoutExposingCredentials() {
        var builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> new KakaoGeocodingClient(builder.build(), "secret")
                .lookup("서울특별시 송파구 올림픽로 300, 2층"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503").hasMessageNotContaining("secret");
        server.verify();
    }
    @Test void missingKeyDoesNotMakeAnExternalRequest() {
        assertThatThrownBy(() -> new KakaoGeocodingClient(RestClient.create(), "").lookup("address"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
    }
}
