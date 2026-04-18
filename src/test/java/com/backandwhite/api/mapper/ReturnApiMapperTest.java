package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.in.ReturnRequestDtoIn;
import com.backandwhite.api.dto.out.ReturnRequestDtoOut;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.valueobject.ReturnStatus;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReturnApiMapperTest {

    private ReturnApiMapperImpl mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ReturnApiMapperImpl();
        Field f = ReturnApiMapperImpl.class.getDeclaredField("moneyMapperHelper");
        f.setAccessible(true);
        f.set(mapper, new MoneyMapperHelper());
    }

    @Test
    void toDto_null_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDto_full() {
        ReturnRequest r = ReturnRequest.builder().id("r1").orderId("o1").userId("u1").status(ReturnStatus.APPROVED)
                .reason("Broken").items(List.of(Map.of("sku", "S1"))).refundAmount(Money.of(new BigDecimal("25")))
                .orderNumber("NX-1").build();
        ReturnRequestDtoOut dto = mapper.toDto(r);
        assertThat(dto.getId()).isEqualTo("r1");
        assertThat(dto.getRefundAmount()).isEqualByComparingTo("25");
        assertThat(dto.getItems()).hasSize(1);
    }

    @Test
    void toDto_nullItems() {
        ReturnRequest r = ReturnRequest.builder().id("r1").items(null).build();
        ReturnRequestDtoOut dto = mapper.toDto(r);
        assertThat(dto.getItems()).isNull();
    }

    @Test
    void toDtoList_null_returnsNull() {
        assertThat(mapper.toDtoList(null)).isNull();
    }

    @Test
    void toDtoList_mapsList() {
        List<ReturnRequestDtoOut> list = mapper.toDtoList(List.of(ReturnRequest.builder().id("a").build()));
        assertThat(list).hasSize(1);
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        ReturnRequestDtoIn dto = new ReturnRequestDtoIn();
        dto.setOrderId("o1");
        dto.setReason("Broken");
        dto.setItems(List.of(Map.of("sku", "S1")));
        dto.setRefundAmount(new BigDecimal("25"));

        ReturnRequest r = mapper.toDomain(dto);
        assertThat(r.getOrderId()).isEqualTo("o1");
        assertThat(r.getItems()).hasSize(1);
        assertThat(r.getRefundAmount().getAmount()).isEqualByComparingTo("25");
    }

    @Test
    void toDomain_nullItems() {
        ReturnRequestDtoIn dto = new ReturnRequestDtoIn();
        dto.setOrderId("o1");
        dto.setReason("R");
        dto.setItems(null);

        ReturnRequest r = mapper.toDomain(dto);
        assertThat(r.getItems()).isNull();
    }
}
