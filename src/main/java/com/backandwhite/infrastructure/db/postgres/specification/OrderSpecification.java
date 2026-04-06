package com.backandwhite.infrastructure.db.postgres.specification;

import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public class OrderSpecification {

    private OrderSpecification() {
    }

    public static Specification<OrderEntity> withFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filters.containsKey("status")) {
                predicate = cb.and(predicate, cb.equal(root.get("status"),
                        com.backandwhite.domain.valueobject.OrderStatus.valueOf(filters.get("status").toString())));
            }
            if (filters.containsKey("userId")) {
                predicate = cb.and(predicate, cb.equal(root.get("userId"), filters.get("userId").toString()));
            }
            if (filters.containsKey("search")) {
                String search = "%" + filters.get("search").toString().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), search),
                        cb.like(cb.lower(root.get("userId")), search)));
            }

            return predicate;
        };
    }
}
