package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.Invoice;
import com.backandwhite.domain.repository.InvoiceRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.InvoiceInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.InvoiceJpaRepository;
import com.backandwhite.infrastructure.db.postgres.specification.InvoiceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {

    private final InvoiceJpaRepository jpa;
    private final InvoiceInfraMapper mapper;

    @Override
    public Invoice save(Invoice invoice) {
        invoice.setId(UUID.randomUUID().toString());
        return mapper.toDomain(jpa.save(mapper.toEntity(invoice)));
    }

    @Override
    public Invoice update(Invoice invoice) {
        return mapper.toDomain(jpa.save(mapper.toEntity(invoice)));
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByOrderId(String orderId) {
        return jpa.findByOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public Page<Invoice> findAll(Map<String, Object> filters, Pageable pageable) {
        return jpa.findAll(InvoiceSpecification.withFilters(filters), pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Invoice> findByUserId(String userId, Pageable pageable) {
        return jpa.findByUserId(userId, pageable).map(mapper::toDomain);
    }
}
