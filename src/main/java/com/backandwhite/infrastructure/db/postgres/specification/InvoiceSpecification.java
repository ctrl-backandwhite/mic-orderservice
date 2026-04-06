package com.backandwhite.infrastructure.db.postgres.specification;

import com.backandwhite.infrastructure.db.postgres.entity.InvoiceEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public class InvoiceSpecification {

    private InvoiceSpecification() {
    }

    public static Specification<InvoiceEntity> withFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filters.containsKey("status")) {
                predicate = cb.and(predicate, cb.equal(root.get("status"),
                        com.backandwhite.domain.valueobject.InvoiceStatus.valueOf(filters.get("status").toString())));
            }
            if (filters.containsKey("orderId")) {
                predicate = cb.and(predicate, cb.equal(root.get("orderId"), filters.get("orderId").toString()));
            }

            return predicate;
        };
    }
}
