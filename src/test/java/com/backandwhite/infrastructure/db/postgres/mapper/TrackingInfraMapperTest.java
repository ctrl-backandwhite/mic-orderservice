package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.infrastructure.db.postgres.entity.TrackingEventEntity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackingInfraMapperTest {

    private TrackingInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrackingInfraMapperImpl();
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        TrackingEventEntity entity = TrackingEventEntity.builder().id("e1").orderId("o1").status("SHIPPED")
                .description("d").location("Madrid").carrier("DHL").eventAt(Instant.EPOCH).build();
        TrackingEvent d = mapper.toDomain(entity);
        assertThat(d.getId()).isEqualTo("e1");
        assertThat(d.getCarrier()).isEqualTo("DHL");
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_full() {
        TrackingEvent d = TrackingEvent.builder().id("e1").orderId("o1").status("SHIPPED").build();
        TrackingEventEntity e = mapper.toEntity(d);
        assertThat(e.getId()).isEqualTo("e1");
    }

    @Test
    void toDomainList_null() {
        assertThat(mapper.toDomainList(null)).isNull();
    }

    @Test
    void toDomainList_mapsList() {
        assertThat(mapper.toDomainList(List.of(TrackingEventEntity.builder().id("e1").build()))).hasSize(1);
    }
}
