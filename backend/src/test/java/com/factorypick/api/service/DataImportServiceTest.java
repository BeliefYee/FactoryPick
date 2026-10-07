package com.factorypick.api.service;

import com.factorypick.api.dto.FactoryRequest;
import com.factorypick.api.repository.DataImportRepository;
import com.factorypick.api.repository.FactoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataImportServiceTest {
    @Test void legacyProductColumnsAreIgnoredWhileFactoryRowsAreImported() {
        var factories = mock(FactoryRepository.class);
        var history = mock(DataImportRepository.class);
        when(factories.findExisting(any(), any(), any())).thenReturn(Optional.empty());
        when(factories.insert(any())).thenReturn(42L);
        var service = new DataImportService(factories, history);
        String csv = "factory_name,company_name,product_name,category,product_description\n"
                + "Factory,Company,Old product,Old category,Old description\n";
        var result = service.importCsv(new MockMultipartFile("file", "legacy.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)));
        assertThat(result.insertedRows()).isEqualTo(1);
        assertThat(result.failedRows()).isZero();
        verify(factories).insert(argThat((FactoryRequest r) ->
                r.factoryName().equals("Factory") && r.companyName().equals("Company")));
        verify(history).insert(eq("legacy.csv"), eq(1), eq(1), eq(0), eq(0), eq(0), eq("SUCCESS"), anyString());
        verifyNoMoreInteractions(history);
    }
}
