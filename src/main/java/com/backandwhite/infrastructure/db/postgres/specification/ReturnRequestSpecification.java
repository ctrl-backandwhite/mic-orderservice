package com.backandwhite.infrastructure.db.postgres.specification;

import com.backandwhite.infrastructure.db.postgres.entity.ReturnRequestEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

public class ReturnRequestSpecification {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_USER_ID = "userId";

    private ReturnRequestSpecification() {
    }

    public static Specification<ReturnRequestEntity> withFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filters.containsKey(FIELD_STATUS)) {
                predicate = cb.and(predicate,
                        cb.equal(root.get(FIELD_STATUS), com.backandwhite.domain.valueobject.ReturnStatus
                                .valueOf(filters.get(FIELD_STATUS).toString())));
            }
            if (filters.containsKey(FIELD_USER_ID)) {
                predicate = cb.and(predicate, cb.equal(root.get(FIELD_USER_ID), filters.get(FIELD_USER_ID).toString()));
            }

            return predicate;
        };
    }
}
