package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Invoice update(Invoice invoice);

    Optional<Invoice> findById(String id);

    Optional<Invoice> findByOrderId(String orderId);

    Page<Invoice> findAll(Map<String, Object> filters, Pageable pageable);

    Page<Invoice> findByUserId(String userId, Pageable pageable);
}
