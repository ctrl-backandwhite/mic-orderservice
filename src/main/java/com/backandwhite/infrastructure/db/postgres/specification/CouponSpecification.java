package com.backandwhite.infrastructure.db.postgres.specification;

import com.backandwhite.infrastructure.db.postgres.entity.CouponEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public class CouponSpecification {

    private CouponSpecification() {
    }

    public static Specification<CouponEntity> withFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filters.containsKey("active")) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("active"), Boolean.valueOf(filters.get("active").toString())));
            }
            if (filters.containsKey("type")) {
                predicate = cb.and(predicate, cb.equal(root.get("type"),
                        com.backandwhite.domain.valueobject.CouponType.valueOf(filters.get("type").toString())));
            }
            if (filters.containsKey("search")) {
                String search = "%" + filters.get("search").toString().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("code")), search));
            }

            return predicate;
        };
    }
}
