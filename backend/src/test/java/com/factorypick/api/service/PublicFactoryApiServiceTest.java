package com.factorypick.api.service;

import com.factorypick.api.repository.FactoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PublicFactoryApiServiceTest {
    FactoryRepository repository = mock(FactoryRepository.class);
    FactoryGeocodingService geocoding = mock(FactoryGeocodingService.class);
    MockRestServiceServer server;
    PublicFactoryApiService service;

    @BeforeEach void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new PublicFactoryApiService(repository, geocoding, builder);
        ReflectionTestUtils.setField(service, "serviceKey", "test%2Bkey%3D");
        ReflectionTestUtils.setField(service, "successCode", "00");
    }
    void respond(String json) {
        server.expect(request -> assertThat(request.getURI().getRawQuery())
                .contains("serviceKey=test%2Bkey%3D").doesNotContain("%25"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
    @Test void rejectsApiErrorWithoutWriting() {
        respond("{\"header\":{\"resultCode\":\"30\"}}");
        assertThatThrownBy(() -> service.importFactories("complex"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(repository, geocoding);
        server.verify();
    }
    @Test void rejectsMissingBody() {
        respond("{\"header\":{\"resultCode\":\"00\"}}");
        assertThatThrownBy(() -> service.importFactories("complex"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(repository);
    }
    @Test void acceptsEmptySuccessfulResult() {
        respond("{\"header\":{\"resultCode\":\"00\"},\"body\":{\"totalCount\":0}}");
        service.importFactories("complex");
        verifyNoInteractions(repository, geocoding);
        server.verify();
    }
    @Test void rejectsIncompletePage() {
        respond("{\"header\":{\"resultCode\":\"00\"},\"body\":{\"totalCount\":1}}");
        assertThatThrownBy(() -> service.importFactories("complex"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(repository);
    }
    @Test void rejectsBlankSearchBeforeCallingApi() {
        assertThatThrownBy(() -> service.importFactories(" "))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
