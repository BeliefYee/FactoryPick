package com.factorypick.api.service;

import com.factorypick.api.dto.FactoryRequest;
import com.factorypick.api.dto.PublicFactoryApiResponse;
import com.factorypick.api.repository.FactoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class PublicFactoryApiService {

    private final FactoryRepository factoryRepository;
    private final FactoryGeocodingService geocodingService;
    private final RestClient restClient;

    @Value("${factorypick.public-data.service-key:${DATA_GO_KR_SERVICE_KEY:}}")
    private String serviceKey;

    public PublicFactoryApiService(
            FactoryRepository factoryRepository,
            FactoryGeocodingService geocodingService
    ) {
        this.factoryRepository = factoryRepository;
        this.geocodingService = geocodingService;
        this.restClient = RestClient.builder().build();
    }

    public void importFactories(String industrialComplexName) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "backend/.env에 DATA_GO_KR_SERVICE_KEY를 설정해 주세요.");
        }

        int pageNo = 1;
        int numOfRows = 100;
        String decodedServiceKey = UriUtils.decode(serviceKey, StandardCharsets.UTF_8);

        while (true) {

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(
                            "https://apis.data.go.kr/B550624/fctryRegistInfo/getFctryListInIrsttService_v2"
                    )
                    .queryParam("serviceKey", decodedServiceKey)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("irsttNm", industrialComplexName)
                    .queryParam("type", "JSON")
                    .build()
                    .encode()
                    .toUri();

            PublicFactoryApiResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(PublicFactoryApiResponse.class);

            if (response.body().items() == null
                    || response.body().items().item() == null
                    || response.body().items().item().isEmpty()) {
                break;
            }

            for (PublicFactoryApiResponse.Item item :
                    response.body().items().item()) {

                saveFactory(item, industrialComplexName);
            }

            if (pageNo * numOfRows >= response.body().totalCount()) {
                break;
            }

            pageNo++;
        }
    }

    private void saveFactory(PublicFactoryApiResponse.Item item, String industrialComplexName) {

        if (item.cmpnyNm() == null || item.cmpnyNm().isBlank()) {
            return;
        }

        if (item.rnAdres() == null || item.rnAdres().isBlank()) {
            return;
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

        var existing =
                factoryRepository.findByManageNo(item.fctryManageNo());

        long factoryId;

        if (existing.isPresent()) {

            factoryId = existing.get().factoryId();

            factoryRepository.update(
                    factoryId,
                    request
            );

        } else {

            factoryId =
                    factoryRepository.insert(request);
        }

        factoryRepository.saveFactoryManageNo(
                factoryId,
                item.fctryManageNo()
        );
        factoryRepository.saveIndustrialComplexName(
            factoryId,
            item.irsttNm() == null || item.irsttNm().isBlank()
                ? industrialComplexName
                : item.irsttNm()
        );
        factoryRepository.savePrimaryIndustryCode(factoryId, item.rprsntvIndutyCode());

        try {
            geocodingService.geocode(factoryId);
        } catch (Exception ignored) {
        }
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
