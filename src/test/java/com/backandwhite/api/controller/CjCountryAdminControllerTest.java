package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.service.CjCountryService;
import com.backandwhite.infrastructure.db.postgres.entity.CjAllowedCountryEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjCountryAdminControllerTest {

    @Mock
    private CjCountryService countryService;

    @InjectMocks
    private CjCountryAdminController controller;

    @Test
    void list_returnsOk() {
        CjAllowedCountryEntity entity = CjAllowedCountryEntity.builder().countryCode("US").countryName("USA")
                .active(true).build();
        when(countryService.listAll()).thenReturn(List.of(entity));
        var resp = controller.list();
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void upsert_returnsOk() {
        CjAllowedCountryEntity saved = CjAllowedCountryEntity.builder().countryCode("US").countryName("USA")
                .active(true).build();
        when(countryService.upsert("US", "USA", true)).thenReturn(saved);
        var resp = controller.upsert("US", new CjCountryAdminController.UpsertBody("USA", true));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(saved);
    }

    @Test
    void deactivate_returnsNoContent() {
        var resp = controller.deactivate("US");
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(countryService).deactivate("US");
    }
}
