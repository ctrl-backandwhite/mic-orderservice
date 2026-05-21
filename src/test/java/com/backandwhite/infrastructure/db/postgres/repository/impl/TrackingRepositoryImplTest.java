package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.infrastructure.db.postgres.entity.TrackingEventEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.TrackingInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.TrackingEventJpaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackingRepositoryImplTest {

    @Mock
    private TrackingEventJpaRepository jpa;

    @Mock
    private TrackingInfraMapper mapper;

    @InjectMocks
    private TrackingRepositoryImpl adapter;

    @Test
    void save_assignsUuidAndDelegates() {
        TrackingEvent in = TrackingEvent.builder().status("CREATED").build();
        TrackingEventEntity entity = new TrackingEventEntity();
        TrackingEventEntity saved = new TrackingEventEntity();
        TrackingEvent out = TrackingEvent.builder().id("x").status("CREATED").build();

        when(mapper.toEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        TrackingEvent result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void findByOrderId_delegates() {
        List<TrackingEventEntity> entities = List.of(new TrackingEventEntity());
        List<TrackingEvent> domain = List.of(TrackingEvent.builder().id("e1").build());
        when(jpa.findByOrderIdOrderByEventAtAsc("ord-1")).thenReturn(entities);
        when(mapper.toDomainList(entities)).thenReturn(domain);

        assertThat(adapter.findByOrderId("ord-1")).isSameAs(domain);
    }
}
