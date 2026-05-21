package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.Invoice;
import com.backandwhite.infrastructure.db.postgres.entity.InvoiceEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.InvoiceInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.InvoiceJpaRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class InvoiceRepositoryImplTest {

    @Mock
    private InvoiceJpaRepository jpa;

    @Mock
    private InvoiceInfraMapper mapper;

    @InjectMocks
    private InvoiceRepositoryImpl adapter;

    private Invoice inv(String id) {
        return Invoice.builder().id(id).invoiceNumber("INV-1").build();
    }

    @Test
    void save_assignsUuidAndDelegates() {
        Invoice in = inv(null);
        InvoiceEntity entity = new InvoiceEntity();
        InvoiceEntity saved = new InvoiceEntity();
        Invoice out = inv("x");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Invoice result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void update_keepsId() {
        Invoice in = inv("kept");
        InvoiceEntity entity = new InvoiceEntity();
        InvoiceEntity saved = new InvoiceEntity();
        Invoice out = inv("kept");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        assertThat(adapter.update(in)).isSameAs(out);
        assertThat(in.getId()).isEqualTo("kept");
    }

    @Test
    void findById_present() {
        InvoiceEntity e = new InvoiceEntity();
        when(jpa.findById("i1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(inv("i1"));
        assertThat(adapter.findById("i1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(jpa.findById("i1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("i1")).isEmpty();
    }

    @Test
    void findByOrderId_present() {
        InvoiceEntity e = new InvoiceEntity();
        when(jpa.findByOrderId("ord-1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(inv("i1"));
        assertThat(adapter.findByOrderId("ord-1")).isPresent();
    }

    @Test
    void findByOrderId_empty() {
        when(jpa.findByOrderId("ord-1")).thenReturn(Optional.empty());
        assertThat(adapter.findByOrderId("ord-1")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_delegates() {
        InvoiceEntity e = new InvoiceEntity();
        Page<InvoiceEntity> p = new PageImpl<>(List.of(e));
        when(jpa.findAll(any(Specification.class), eq(PageRequest.of(0, 5)))).thenReturn(p);
        when(mapper.toDomain(e)).thenReturn(inv("i1"));

        assertThat(adapter.findAll(Map.of("status", "PAID"), PageRequest.of(0, 5)).getContent()).hasSize(1);
    }

    @Test
    void findByUserId_delegates() {
        InvoiceEntity e = new InvoiceEntity();
        Page<InvoiceEntity> p = new PageImpl<>(List.of(e));
        when(jpa.findByUserId("u1", PageRequest.of(0, 5))).thenReturn(p);
        when(mapper.toDomain(e)).thenReturn(inv("i1"));

        assertThat(adapter.findByUserId("u1", PageRequest.of(0, 5)).getContent()).hasSize(1);
    }
}
