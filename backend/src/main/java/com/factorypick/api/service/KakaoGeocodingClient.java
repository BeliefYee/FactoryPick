package com.factorypick.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class KakaoGeocodingClient {
    private final RestClient client;
    private final String key;
    public record Result(String status, BigDecimal latitude, BigDecimal longitude) {}

    @org.springframework.beans.factory.annotation.Autowired
    public KakaoGeocodingClient(RestClient.Builder builder,
            @Value("${factorypick.kakao.rest-api-key:}") String key) {
        this.key = key.trim();
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = builder.baseUrl("https://dapi.kakao.com").requestFactory(factory).build();
    }

    KakaoGeocodingClient(RestClient client, String key) { this.client = client; this.key = key; }

    public Result lookup(String address) {
        if (key.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "backend/.env에 KAKAO_REST_API_KEY를 설정해 주세요.");
        if (address == null || address.isBlank()) return new Result("REVIEW", null, null);
        try {
            JsonNode body = client.get().uri(uri -> uri.path("/v2/local/search/address.json")
                    .queryParam("query", "{address}").queryParam("analyze_type", "exact")
                    .queryParam("size", 2).build(address.trim()))
                    .header("Authorization", "KakaoAK " + key).retrieve().body(JsonNode.class);
            if (body == null || !body.path("documents").isArray() || !body.path("meta").has("total_count"))
                return new Result("FAILED", null, null);
            int count = body.path("meta").path("total_count").asInt(-1);
            JsonNode documents = body.path("documents");
            if (count == 0 && documents.isEmpty()) return new Result("NOT_FOUND", null, null);
            if (count != 1 || documents.size() != 1) return new Result("REVIEW", null, null);
            JsonNode item = documents.get(0);
            String type = item.path("address_type").asText();
            if (!type.equals("ROAD_ADDR") && !type.equals("REGION_ADDR")) return new Result("REVIEW", null, null);
            BigDecimal latitude = new BigDecimal(item.path("y").asText());
            BigDecimal longitude = new BigDecimal(item.path("x").asText());
            if (latitude.compareTo(BigDecimal.valueOf(33)) < 0 || latitude.compareTo(BigDecimal.valueOf(39)) > 0
                    || longitude.compareTo(BigDecimal.valueOf(124)) < 0 || longitude.compareTo(BigDecimal.valueOf(132)) > 0)
                return new Result("REVIEW", null, null);
            return new Result("RESOLVED", latitude, longitude);
        } catch (RestClientException | NumberFormatException e) {
            // Do not expose provider request headers or credentials in API errors.
            return new Result("FAILED", null, null);
        }
    }
}
