package com.backandwhite.application.usecase;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.domain.model.Invoice;
import java.util.Map;
import java.util.Optional;

public interface InvoiceUseCase {
    Invoice create(Invoice invoice);

    Invoice findById(String id);

    Invoice findByOrderId(String orderId);

    /**
     * Non-throwing variant used by the order-confirmation flow to check idempotency
     * without forcing the outer @Transactional to roll back when the invoice
     * doesn't exist yet.
     */
    Optional<Invoice> findOptionalByOrderId(String orderId);

    PageResult<Invoice> findAll(Map<String, Object> filters, int page, int size, String sortBy, boolean ascending);

    PageResult<Invoice> findByUserId(String userId, int page, int size, String sortBy, boolean ascending);

    Invoice update(String id, Invoice invoice);
}
