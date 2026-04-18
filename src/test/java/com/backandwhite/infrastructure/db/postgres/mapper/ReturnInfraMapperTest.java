package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.valueobject.ReturnStatus;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import com.backandwhite.infrastructure.db.postgres.entity.ReturnRequestEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReturnInfraMapperTest {

    private ReturnInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReturnInfraMapperImpl();
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        OrderEntity order = OrderEntity.builder().id("o1").orderNumber("NX-1").build();
        ReturnRequestEntity entity = ReturnRequestEntity.builder().id("r1").orderId("o1").order(order).userId("u1")
                .status(ReturnStatus.APPROVED).reason("broken").items(List.of(Map.of("x", "y")))
                .refundAmount(Money.of(new BigDecimal("25"))).build();
        ReturnRequest d = mapper.toDomain(entity);
        assertThat(d.getId()).isEqualTo("r1");
        assertThat(d.getOrderNumber()).isEqualTo("NX-1");
        assertThat(d.getItems()).hasSize(1);
    }

    @Test
    void toDomain_nullOrderAndItems() {
        ReturnRequestEntity entity = ReturnRequestEntity.builder().id("r1").order(null).items(null).build();
        ReturnRequest d = mapper.toDomain(entity);
        assertThat(d.getOrderNumber()).isNull();
        assertThat(d.getItems()).isNull();
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_full() {
        ReturnRequest d = ReturnRequest.builder().id("r1").orderId("o1").userId("u1").status(ReturnStatus.APPROVED)
                .items(List.of(Map.of("a", "b"))).refundAmount(Money.of(new BigDecimal("25"))).build();
        ReturnRequestEntity e = mapper.toEntity(d);
        assertThat(e.getId()).isEqualTo("r1");
        assertThat(e.getItems()).hasSize(1);
    }

    @Test
    void toEntity_nullItems() {
        ReturnRequest d = ReturnRequest.builder().id("r1").items(null).build();
        ReturnRequestEntity e = mapper.toEntity(d);
        assertThat(e.getItems()).isNull();
    }
}
