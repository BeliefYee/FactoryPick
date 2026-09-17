package com.factorypick.api.service;

import com.factorypick.api.dto.FactoryRequest;
import com.factorypick.api.dto.PublicFactoryApiResponse;
import com.factorypick.api.repository.FactoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class PublicFactoryApiService {

    private final FactoryRepository factoryRepository;
    private final FactoryGeocodingService geocodingService;
    private final RestClient restClient;
    @Value("${factorypick.public-data.success-code:00}")
    private String successCode;

    @Value("${factorypick.public-data.service-key:}")
    private String serviceKey;

    public PublicFactoryApiService(
            FactoryRepository factoryRepository,
            FactoryGeocodingService geocodingService, RestClient.Builder builder
    ) {
        this.factoryRepository = factoryRepository;
        this.geocodingService = geocodingService;
        this.restClient = builder.build();
    }

    public void importFactories(String industrialComplexName) {

        if (industrialComplexName == null || industrialComplexName.isBlank()) {
            throw new IllegalArgumentException("Industrial complex name is required.");
        }
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("Public API service key is not configured.");
        }
        int pageNo = 1;
        int numOfRows = 100;

        while (true) {

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(
                            "https://apis.data.go.kr/B550624/fctryRegistInfo/getFctryListInIrsttService_v2"
                    )
                    .queryParam("serviceKey", "{serviceKey}")
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("irsttNm", industrialComplexName)
                    .queryParam("type", "JSON")
                    .encode()
                    .buildAndExpand(decodedServiceKey())
                    .toUri();

            PublicFactoryApiResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(PublicFactoryApiResponse.class);

            if (response == null || response.header() == null) {
                throw new IllegalStateException("Invalid public API response.");
            }
            if (!successCode.equals(response.header().resultCode())) {
                throw new IllegalStateException("Public API returned an error code.");
            }
            if (response.body() == null) {
                throw new IllegalStateException("Public API response body is missing.");
            }
            if (response.body().items() == null
                    || response.body().items().item() == null
                    || response.body().items().item().isEmpty()) {
                if (response.body().totalCount() > (long) (pageNo - 1) * numOfRows) {
                    throw new IllegalStateException("Public API returned an incomplete page.");
                }
                break;
            }

            for (PublicFactoryApiResponse.Item item :
                    response.body().items().item()) {

                saveFactory(item);
            }

            if (pageNo * numOfRows >= response.body().totalCount()) {
                break;
            }

            pageNo++;
        }
    }

    private void saveFactory(PublicFactoryApiResponse.Item item) {

        if (item.cmpnyNm() == null || item.cmpnyNm().isBlank()
                || item.fctryManageNo() == null || item.fctryManageNo().isBlank()) {
            throw new IllegalStateException("Public API item is missing its name or management number.");
        }

        String factoryName = item.cmpnyNm();

        FactoryRequest request = new FactoryRequest(
                null,
                factoryName,
                item.cmpnyNm(),
                item.rnAdres(),
                getSido(item.rnAdres()),
                getSigungu(item.rnAdres()),
                null,
                null,
                item.indutyNm(),
                getYear(item.frstFctryRegistDe()),
                null,
                item.cmpnyTelno()
        );

        long factoryId = factoryRepository.saveFromPublicApi(request, item.fctryManageNo());

        if (item.rnAdres() != null
                && !item.rnAdres().isBlank()) {

            try {
                geocodingService.geocode(factoryId);
            } catch (Exception ignored) {
            }
        }
    }

    private String decodedServiceKey() {
        return serviceKey.contains("%")
                ? java.net.URLDecoder.decode(serviceKey, java.nio.charset.StandardCharsets.UTF_8)
                : serviceKey;
    }

    private Integer getYear(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {

            String year = value.trim();

            if (year.length() >= 4) {
                year = year.substring(0, 4);
            }

            int result = Integer.parseInt(year);

            if (result >= 1800 && result <= 2100) {
                return result;
            }

        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    private String getSido(String address) {

        if (address == null || address.isBlank()) {
            return null;
        }

        String[] parts =
                address.trim().split("\\s+");

        return parts.length >= 1
                ? parts[0]
                : null;
    }

    private String getSigungu(String address) {

        if (address == null || address.isBlank()) {
            return null;
        }

        String[] parts =
                address.trim().split("\\s+");

        return parts.length >= 2
                ? parts[1]
                : null;
    }
}