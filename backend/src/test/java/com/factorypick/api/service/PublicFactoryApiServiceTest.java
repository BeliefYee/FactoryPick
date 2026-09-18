package com.factorypick.api.service;

import com.factorypick.api.repository.FactoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PublicFactoryApiServiceTest {
    @Test
    void missingKeyAllowsStartupButRejectsImport() {
        var repository = mock(FactoryRepository.class);
        var geocoding = mock(FactoryGeocodingService.class);
        new ApplicationContextRunner()
                .withPropertyValues("factorypick.public-data.service-key=")
                .withBean(FactoryRepository.class, () -> repository)
                .withBean(FactoryGeocodingService.class, () -> geocoding)
                .withBean(PublicFactoryApiService.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var service = context.getBean(PublicFactoryApiService.class);
                    assertThatThrownBy(() -> service.importFactories("test"))
                            .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                                assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                                assertThat(error.getReason()).contains("DATA_GO_KR_SERVICE_KEY");
                            });
                    verifyNoInteractions(repository, geocoding);
                });
    }
}
