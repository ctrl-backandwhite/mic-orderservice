package com.backandwhite.infrastructure.db.postgres.specification;

import com.backandwhite.infrastructure.db.postgres.entity.ReturnRequestEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

public class ReturnRequestSpecification {

    private ReturnRequestSpecification() {
    }

    public static Specification<ReturnRequestEntity> withFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filters.containsKey("status")) {
                predicate = cb.and(predicate, cb.equal(root.get("status"),
                        com.backandwhite.domain.valueobject.ReturnStatus.valueOf(filters.get("status").toString())));
            }
            if (filters.containsKey("userId")) {
                predicate = cb.and(predicate, cb.equal(root.get("userId"), filters.get("userId").toString()));
            }

            return predicate;
        };
    }
}
