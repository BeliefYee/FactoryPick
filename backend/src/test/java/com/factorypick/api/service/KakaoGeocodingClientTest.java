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
        assertThat(new KakaoGeocodingClient(builder.build(), "secret").lookup("address").status()).isEqualTo("FAILED");
    }
    @Test void missingKeyDoesNotMakeAnExternalRequest() {
        assertThatThrownBy(() -> new KakaoGeocodingClient(RestClient.create(), "").lookup("address"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
    }
}
