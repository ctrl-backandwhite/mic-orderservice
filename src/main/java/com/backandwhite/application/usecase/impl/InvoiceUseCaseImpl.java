package com.backandwhite.application.usecase.impl;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.InvoiceUseCase;
import com.backandwhite.domain.model.Invoice;
import com.backandwhite.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class InvoiceUseCaseImpl implements InvoiceUseCase {

    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public Invoice create(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice findById(String id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Invoice", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice findByOrderId(String orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Invoice", orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Invoice> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(invoiceRepository.findAll(filters, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Invoice> findByUserId(String userId, int page, int size, String sortBy, boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(invoiceRepository.findByUserId(userId, pageable));
    }

    @Override
    @Transactional
    public Invoice update(String id, Invoice invoice) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Invoice", id));
        invoice.setId(id);
        // Preserve fields not present in the update DTO
        if (invoice.getInvoiceNumber() == null) {
            invoice.setInvoiceNumber(existing.getInvoiceNumber());
        }
        return invoiceRepository.update(invoice);
    }
}
