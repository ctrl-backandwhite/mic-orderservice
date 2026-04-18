package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.in.TrackingEventDtoIn;
import com.backandwhite.domain.model.TrackingEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackingApiMapperTest {

    private TrackingApiMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrackingApiMapperImpl();
    }

    @Test
    void toDto_null() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDto_full() {
        TrackingEvent e = TrackingEvent.builder().id("e1").orderId("o1").status("SHIPPED").description("d")
                .location("Madrid").carrier("DHL").eventAt(Instant.EPOCH).build();
        var dto = mapper.toDto(e);
        assertThat(dto.getId()).isEqualTo("e1");
        assertThat(dto.getOrderId()).isEqualTo("o1");
        assertThat(dto.getCarrier()).isEqualTo("DHL");
    }

    @Test
    void toDtoList_null() {
        assertThat(mapper.toDtoList(null)).isNull();
    }

    @Test
    void toDtoList_mapsList() {
        assertThat(mapper.toDtoList(List.of(TrackingEvent.builder().id("e1").build()))).hasSize(1);
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        TrackingEventDtoIn dto = new TrackingEventDtoIn();
        dto.setOrderId("o1");
        dto.setStatus("SHIPPED");
        dto.setDescription("d");
        dto.setLocation("Madrid");
        dto.setCarrier("DHL");
        dto.setEventAt(Instant.EPOCH);

        TrackingEvent e = mapper.toDomain(dto);
        assertThat(e.getOrderId()).isEqualTo("o1");
        assertThat(e.getStatus()).isEqualTo("SHIPPED");
    }
}
