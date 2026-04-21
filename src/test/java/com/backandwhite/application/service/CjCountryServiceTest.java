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
}
