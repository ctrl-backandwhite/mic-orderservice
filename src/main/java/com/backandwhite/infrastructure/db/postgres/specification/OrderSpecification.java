package com.backandwhite.infrastructure.db.postgres.specification;

import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecification {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_USER_ID = "userId";

    private OrderSpecification() {
    }

    public static Specification<OrderEntity> withFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filters.containsKey(FIELD_STATUS)) {
                predicate = cb.and(predicate, cb.equal(root.get(FIELD_STATUS),
                        com.backandwhite.domain.valueobject.OrderStatus.valueOf(filters.get(FIELD_STATUS).toString())));
            }
            if (filters.containsKey(FIELD_USER_ID)) {
                predicate = cb.and(predicate, cb.equal(root.get(FIELD_USER_ID), filters.get(FIELD_USER_ID).toString()));
            }
            if (filters.containsKey("search")) {
                String search = "%" + filters.get("search").toString().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(cb.like(cb.lower(root.get("orderNumber")), search),
                        cb.like(cb.lower(root.get(FIELD_USER_ID)), search)));
            }

            return predicate;
        };
    }
}
