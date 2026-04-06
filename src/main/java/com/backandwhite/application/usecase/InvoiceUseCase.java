package com.backandwhite.application.usecase;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.domain.model.Invoice;

import java.util.Map;

public interface InvoiceUseCase {
    Invoice create(Invoice invoice);

    Invoice findById(String id);

    Invoice findByOrderId(String orderId);

    PageResult<Invoice> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending);

    PageResult<Invoice> findByUserId(String userId, int page, int size, String sortBy, boolean ascending);

    Invoice update(String id, Invoice invoice);
}
