package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.AppSettingEntity;
import com.backandwhite.infrastructure.db.postgres.repository.AppSettingJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppSettingsAdminControllerTest {

    @Mock
    private AppSettingJpaRepository repository;

    @InjectMocks
    private AppSettingsAdminController controller;

    @Test
    void get_existingKey_returnsValue() {
        AppSettingEntity entity = AppSettingEntity.builder().key("CJ_ENABLED").value("true").build();
        when(repository.findById("CJ_ENABLED")).thenReturn(Optional.of(entity));
        var resp = controller.get("CJ_ENABLED");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("key", "CJ_ENABLED").containsEntry("value", "true");
    }

    @Test
    void get_existingKey_nullValue_returnsEmptyString() {
        AppSettingEntity entity = AppSettingEntity.builder().key("X").value(null).build();
        when(repository.findById("X")).thenReturn(Optional.of(entity));
        var resp = controller.get("X");
        assertThat(resp.getBody()).containsEntry("value", "");
    }

    @Test
    void get_missingKey_returnsEmptyValue() {
        when(repository.findById("MISSING")).thenReturn(Optional.empty());
        var resp = controller.get("MISSING");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("key", "MISSING").containsEntry("value", "");
    }

    @Test
    void put_existingKey_updates() {
        AppSettingEntity existing = AppSettingEntity.builder().key("K").value("old").build();
        when(repository.findById("K")).thenReturn(Optional.of(existing));
        var resp = controller.put("K", "new");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("key", "K").containsEntry("value", "new");
        verify(repository).save(any(AppSettingEntity.class));
    }

    @Test
    void put_missingKey_creates() {
        when(repository.findById("NEW")).thenReturn(Optional.empty());
        var resp = controller.put("NEW", "v");
        assertThat(resp.getBody()).containsEntry("key", "NEW").containsEntry("value", "v");
        verify(repository).save(any(AppSettingEntity.class));
    }
}
