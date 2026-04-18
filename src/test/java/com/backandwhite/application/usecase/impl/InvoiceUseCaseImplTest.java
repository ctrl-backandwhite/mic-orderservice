package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.Invoice;
import com.backandwhite.domain.repository.InvoiceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InvoiceUseCaseImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceUseCaseImpl useCase;

    @Test
    void create_savesAndReturnsInvoice() {
        Invoice invoice = Invoice.builder().id("i1").build();
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        Invoice result = useCase.create(invoice);

        assertThat(result).isSameAs(invoice);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void findById_existingId_returnsInvoice() {
        Invoice invoice = Invoice.builder().id("i1").build();
        when(invoiceRepository.findById("i1")).thenReturn(Optional.of(invoice));
        assertThat(useCase.findById("i1")).isSameAs(invoice);
    }

    @Test
    void findById_missingId_throwsEntityNotFound() {
        when(invoiceRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findById("missing")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByOrderId_existing_returns() {
        Invoice invoice = Invoice.builder().id("i1").orderId("o1").build();
        when(invoiceRepository.findByOrderId("o1")).thenReturn(Optional.of(invoice));
        assertThat(useCase.findByOrderId("o1")).isSameAs(invoice);
    }

    @Test
    void findByOrderId_missing_throws() {
        when(invoiceRepository.findByOrderId("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findByOrderId("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAll_ascending_delegates() {
        Page<Invoice> page = new PageImpl<>(List.of(Invoice.builder().id("i1").build()));
        when(invoiceRepository.findAll(any(), any(Pageable.class))).thenReturn(page);

        var result = useCase.findAll(Map.of(), 0, 10, "issueDate", true);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void findAll_descending_delegates() {
        Page<Invoice> page = new PageImpl<>(List.of());
        when(invoiceRepository.findAll(any(), any(Pageable.class))).thenReturn(page);

        var result = useCase.findAll(Map.of(), 1, 5, "id", false);
        assertThat(result.content()).isEmpty();
    }

    @Test
    void findByUserId_ascending_delegates() {
        Page<Invoice> page = new PageImpl<>(List.of());
        when(invoiceRepository.findByUserId(eq("u1"), any(Pageable.class))).thenReturn(page);

        var result = useCase.findByUserId("u1", 0, 10, "createdAt", true);
        assertThat(result).isNotNull();
    }

    @Test
    void findByUserId_descending_delegates() {
        Page<Invoice> page = new PageImpl<>(List.of());
        when(invoiceRepository.findByUserId(eq("u1"), any(Pageable.class))).thenReturn(page);

        var result = useCase.findByUserId("u1", 0, 10, "createdAt", false);
        assertThat(result).isNotNull();
    }

    @Test
    void update_existing_updates() {
        Invoice existing = Invoice.builder().id("i1").invoiceNumber("FAC-001").build();
        Invoice updateDto = Invoice.builder().orderId("o1").build();
        Invoice saved = Invoice.builder().id("i1").orderId("o1").invoiceNumber("FAC-001").build();
        when(invoiceRepository.findById("i1")).thenReturn(Optional.of(existing));
        when(invoiceRepository.update(any(Invoice.class))).thenReturn(saved);

        Invoice result = useCase.update("i1", updateDto);

        assertThat(result).isSameAs(saved);
        assertThat(updateDto.getId()).isEqualTo("i1");
        assertThat(updateDto.getInvoiceNumber()).isEqualTo("FAC-001");
    }

    @Test
    void update_preservesInvoiceNumberWhenProvided() {
        Invoice existing = Invoice.builder().id("i1").invoiceNumber("FAC-001").build();
        Invoice updateDto = Invoice.builder().invoiceNumber("FAC-002").build();
        when(invoiceRepository.findById("i1")).thenReturn(Optional.of(existing));
        when(invoiceRepository.update(any(Invoice.class))).thenReturn(updateDto);

        useCase.update("i1", updateDto);

        assertThat(updateDto.getInvoiceNumber()).isEqualTo("FAC-002");
    }

    @Test
    void update_missing_throws() {
        when(invoiceRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.update("x", Invoice.builder().build()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
