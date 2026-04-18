package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.in.InvoiceDtoIn;
import com.backandwhite.api.dto.out.InvoiceDtoOut;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Invoice;
import com.backandwhite.domain.valueobject.InvoiceStatus;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceApiMapperTest {

    private InvoiceApiMapperImpl mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new InvoiceApiMapperImpl();
        Field f = InvoiceApiMapperImpl.class.getDeclaredField("moneyMapperHelper");
        f.setAccessible(true);
        f.set(mapper, new MoneyMapperHelper());
    }

    @Test
    void toDto_null_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDto_full() {
        Invoice i = Invoice.builder().id("i1").invoiceNumber("INV-001").orderId("o1").status(InvoiceStatus.PAID)
                .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(30))
                .subtotal(Money.of(new BigDecimal("100"))).tax(Money.of(new BigDecimal("10")))
                .total(Money.of(new BigDecimal("110"))).customerSnapshot(Map.of("name", "John"))
                .lines(List.of(Map.of("desc", "Item"))).notes("N").orderNumber("NX-1").build();

        InvoiceDtoOut dto = mapper.toDto(i);
        assertThat(dto.getId()).isEqualTo("i1");
        assertThat(dto.getOrderId()).isEqualTo("o1");
        assertThat(dto.getSubtotal()).isEqualByComparingTo("100");
    }

    @Test
    void toDtoList_null_returnsNull() {
        assertThat(mapper.toDtoList(null)).isNull();
    }

    @Test
    void toDtoList_mapsList() {
        List<InvoiceDtoOut> list = mapper.toDtoList(List.of(Invoice.builder().id("a").build()));
        assertThat(list).hasSize(1);
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        InvoiceDtoIn dto = new InvoiceDtoIn();
        dto.setOrderId("o1");
        dto.setStatus(InvoiceStatus.PAID);
        dto.setIssueDate(LocalDate.now());
        dto.setDueDate(LocalDate.now().plusDays(30));
        dto.setSubtotal(new BigDecimal("100"));
        dto.setShipping(new BigDecimal("5"));
        dto.setTax(new BigDecimal("10"));
        dto.setTotal(new BigDecimal("115"));
        dto.setPaymentMethod("card");
        dto.setCustomerSnapshot(Map.of("name", "John"));
        dto.setLines(List.of(Map.of("desc", "Item")));
        dto.setNotes("N");

        Invoice i = mapper.toDomain(dto);
        assertThat(i.getOrderId()).isEqualTo("o1");
        assertThat(i.getSubtotal().getAmount()).isEqualByComparingTo("100");
        assertThat(i.getLines()).hasSize(1);
    }

    @Test
    void toDomain_nullMaps_skipped() {
        InvoiceDtoIn dto = new InvoiceDtoIn();
        dto.setOrderId("o1");
        dto.setStatus(InvoiceStatus.PENDING);
        Invoice i = mapper.toDomain(dto);
        assertThat(i.getCustomerSnapshot()).isNull();
        assertThat(i.getLines()).isNull();
    }
}
