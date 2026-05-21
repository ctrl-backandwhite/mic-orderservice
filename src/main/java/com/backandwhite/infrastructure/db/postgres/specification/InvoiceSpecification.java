package com.backandwhite.infrastructure.db.postgres.specification;

import com.backandwhite.infrastructure.db.postgres.entity.InvoiceEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

public class InvoiceSpecification {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ORDER_ID = "orderId";

    private InvoiceSpecification() {
    }

    public static Specification<InvoiceEntity> withFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filters.containsKey(FIELD_STATUS)) {
                predicate = cb.and(predicate,
                        cb.equal(root.get(FIELD_STATUS), com.backandwhite.domain.valueobject.InvoiceStatus
                                .valueOf(filters.get(FIELD_STATUS).toString())));
            }
            if (filters.containsKey(FIELD_ORDER_ID)) {
                predicate = cb.and(predicate,
                        cb.equal(root.get(FIELD_ORDER_ID), filters.get(FIELD_ORDER_ID).toString()));
            }

            return predicate;
        };
    }
}
