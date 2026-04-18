package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Invoice;
import com.backandwhite.domain.valueobject.InvoiceStatus;
import com.backandwhite.infrastructure.db.postgres.entity.InvoiceEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceInfraMapperTest {

    private InvoiceInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new InvoiceInfraMapperImpl();
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        OrderEntity order = OrderEntity.builder().id("o1").orderNumber("NX-1").build();
        InvoiceEntity entity = InvoiceEntity.builder().id("i1").invoiceNumber("INV-1").orderId("o1").order(order)
                .status(InvoiceStatus.PAID).issueDate(LocalDate.now()).subtotal(Money.of(new BigDecimal("100")))
                .customerSnapshot(Map.of("name", "John")).lines(List.of(Map.of("desc", "Item"))).build();
        Invoice d = mapper.toDomain(entity);
        assertThat(d.getId()).isEqualTo("i1");
        assertThat(d.getOrderNumber()).isEqualTo("NX-1");
        assertThat(d.getLines()).hasSize(1);
    }

    @Test
    void toDomain_nullMapsAndNullOrder() {
        InvoiceEntity entity = InvoiceEntity.builder().id("i1").order(null).customerSnapshot(null).lines(null).build();
        Invoice d = mapper.toDomain(entity);
        assertThat(d.getOrderNumber()).isNull();
        assertThat(d.getCustomerSnapshot()).isNull();
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_full() {
        Invoice d = Invoice.builder().id("i1").invoiceNumber("INV-1").orderId("o1").status(InvoiceStatus.PAID)
                .customerSnapshot(Map.of("n", "J")).lines(List.of(Map.of("d", "x"))).build();
        InvoiceEntity e = mapper.toEntity(d);
        assertThat(e.getId()).isEqualTo("i1");
        assertThat(e.getLines()).hasSize(1);
    }

    @Test
    void toEntity_nullCollections() {
        Invoice d = Invoice.builder().id("i1").customerSnapshot(null).lines(null).build();
        InvoiceEntity e = mapper.toEntity(d);
        assertThat(e.getCustomerSnapshot()).isNull();
    }
}
