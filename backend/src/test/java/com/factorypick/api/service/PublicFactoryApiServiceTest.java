package com.factorypick.api.service;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.dto.FactoryRequest;
import com.factorypick.api.repository.FactoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PublicFactoryApiServiceTest {
    private final FactoryRepository factories = mock(FactoryRepository.class);
    private final FactoryGeocodingService geocoding = mock(FactoryGeocodingService.class);
    private PublicFactoryApiService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new PublicFactoryApiService(factories, geocoding);
        ReflectionTestUtils.setField(service, "restClient", builder.build());
        ReflectionTestUtils.setField(service, "serviceKey", "test-key");
    }

    @Test
    void serviceStartsWithoutPublicDataKey() {
        new ApplicationContextRunner()
                .withUserConfiguration(PublicFactoryApiService.class, PropertySourcesPlaceholderConfigurer.class)
                .withBean(FactoryRepository.class, () -> factories)
                .withBean(FactoryGeocodingService.class, () -> geocoding)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(PublicFactoryApiService.class);
                    assertThatThrownBy(() -> context.getBean(PublicFactoryApiService.class)
                            .importFactories("산업단지"))
                            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
                });
        verifyNoInteractions(factories, geocoding);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingKeyRejectsImportBeforeExternalRequests(String key) {
        ReflectionTestUtils.setField(service, "serviceKey", key);

        assertThatThrownBy(() -> service.importFactories("산업단지"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getReason()).contains("DATA_GO_KR_SERVICE_KEY");
                });

        verifyNoInteractions(factories, geocoding);
        server.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void missingAddressSkipsAllDatabaseAndGeocodingOperations(String address) throws Exception {
        String addressJson = new ObjectMapper().writeValueAsString(address);
        server.expect(queryParam("pageNo", "1"))
                .andRespond(withSuccess("""
                        {"body":{"totalCount":1,"items":{"item":[
                          {"fctryManageNo":"missing","cmpnyNm":"공장","rnAdres":%s}
                        ]}}}
                        """.formatted(addressJson), MediaType.APPLICATION_JSON));

        service.importFactories("산업단지");

        verifyNoInteractions(factories, geocoding);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void continuesToNextPageAfterSkippingMissingAddressesAndSavesValidFactory(boolean existing) {
        server.expect(queryParam("pageNo", "1"))
                .andRespond(withSuccess("""
                        {"body":{"totalCount":101,"items":{"item":[
                          {"fctryManageNo":"missing","cmpnyNm":"주소 없는 공장"}
                        ]}}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(queryParam("pageNo", "2"))
                .andRespond(withSuccess("""
                        {"body":{"totalCount":101,"items":{"item":[
                          {"fctryManageNo":"valid","cmpnyNm":"정상 공장",
                           "rnAdres":"서울특별시 송파구 올림픽로 300","rprsntvIndutyCode":"26111"}
                        ]}}}
                        """, MediaType.APPLICATION_JSON));
        if (existing) {
            var factory = mock(Factory.class);
            when(factory.factoryId()).thenReturn(42L);
            when(factories.findByManageNo("valid")).thenReturn(Optional.of(factory));
        } else {
            when(factories.insert(any(FactoryRequest.class))).thenReturn(42L);
        }

        service.importFactories("산업단지");

        var request = ArgumentCaptor.forClass(FactoryRequest.class);
        verify(factories).findByManageNo("valid");
        if (existing) {
            verify(factories).update(eq(42L), request.capture());
        } else {
            verify(factories).insert(request.capture());
        }
        assertThat(request.getValue().address()).isEqualTo("서울특별시 송파구 올림픽로 300");
        verify(factories).saveFactoryManageNo(42L, "valid");
        verify(factories).saveIndustrialComplexName(42L, "산업단지");
        verify(factories).savePrimaryIndustryCode(42L, "26111");
        verify(geocoding).geocode(42L);
        verifyNoMoreInteractions(factories, geocoding);
        server.verify();
    }
}
