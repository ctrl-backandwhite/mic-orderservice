package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.CjAllowedCountryEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjAllowedCountryJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjCountryService")
class CjCountryServiceTest {

    @Mock
    private CjAllowedCountryJpaRepository repository;

    @InjectMocks
    private CjCountryService service;

    @Test
    @DisplayName("normalises input country code before lookup")
    void normalisesInput() {
        when(repository.existsByCountryCodeAndActiveTrue("MX")).thenReturn(true);
        assertThat(service.isAllowed(" mx ")).isTrue();
    }

    @Test
    @DisplayName("rejects malformed country codes without hitting the DB")
    void rejectsMalformed() {
        assertThat(service.isAllowed(null)).isFalse();
        assertThat(service.isAllowed("MEX")).isFalse();
        assertThat(service.isAllowed("")).isFalse();
    }

    @Test
    @DisplayName("upserts with normalised code and saves both create and update paths")
    void upsertsCountry() {
        when(repository.findById("DE")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(CjAllowedCountryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.upsert(" de ", "Germany", true);

        ArgumentCaptor<CjAllowedCountryEntity> captor = ArgumentCaptor.forClass(CjAllowedCountryEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCountryCode()).isEqualTo("DE");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("listActive delegates to the repository's active-only finder")
    void listActiveDelegates() {
        when(repository.findAllByActiveTrueOrderByCountryCodeAsc()).thenReturn(java.util.List.of());
        assertThat(service.listActive()).isEmpty();
    }

    @Test
    @DisplayName("listAll delegates to repository.findAll")
    void listAllDelegates() {
        when(repository.findAll()).thenReturn(java.util.List.of());
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    @DisplayName("upsert throws when the input country code cannot be normalised")
    void upsertRejectsBadCode() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.upsert("MEX", "name", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deactivate is a no-op when the country code is malformed")
    void deactivateBadCode() {
        service.deactivate("MEX");
        verify(repository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("deactivate flips active=false and saves when the entry exists")
    void deactivateExistingCountry() {
        CjAllowedCountryEntity entity = CjAllowedCountryEntity.builder().countryCode("DE").active(true).build();
        when(repository.findById("DE")).thenReturn(Optional.of(entity));
        service.deactivate("de");
        ArgumentCaptor<CjAllowedCountryEntity> captor = ArgumentCaptor.forClass(CjAllowedCountryEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivate is a no-op when the country code is not registered")
    void deactivateMissingCountry() {
        when(repository.findById("DE")).thenReturn(Optional.empty());
        service.deactivate("DE");
        verify(repository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("upsert reuses the stored entity when the country already exists")
    void upsertExistingCountry() {
        CjAllowedCountryEntity existing = CjAllowedCountryEntity.builder().countryCode("DE").countryName("Old").build();
        when(repository.findById("DE")).thenReturn(Optional.of(existing));
        when(repository.save(org.mockito.ArgumentMatchers.any(CjAllowedCountryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        service.upsert("de", null, false);
        ArgumentCaptor<CjAllowedCountryEntity> captor = ArgumentCaptor.forClass(CjAllowedCountryEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCountryName()).isEqualTo("DE"); // null -> code fallback
        assertThat(captor.getValue().isActive()).isFalse();
    }
}
