package com.backandwhite.infrastructure.db.postgres.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.valueobject.CouponType;
import com.backandwhite.infrastructure.db.postgres.entity.CouponEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CouponSpecificationTest {

    @Mock
    private Root<CouponEntity> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Predicate basePredicate;

    @Mock
    private Predicate equalPredicate;

    @Mock
    private Predicate likePredicate;

    @Mock
    private Predicate combined;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path activePath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path typePath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path codePath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Expression lowerCode;

    @BeforeEach
    void setUp() {
        lenient().when(cb.conjunction()).thenReturn(basePredicate);
        lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(combined);
    }

    @Test
    void withFilters_emptyMap_returnsConjunctionOnly() {
        Specification<CouponEntity> spec = CouponSpecification.withFilters(new HashMap<>());

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(basePredicate);
        verify(cb).conjunction();
        verify(cb, never()).and(any(Predicate.class), any(Predicate.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_active_addsBooleanEqualPredicate() {
        when(root.get("active")).thenReturn(activePath);
        when(cb.equal(activePath, Boolean.TRUE)).thenReturn(equalPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("active", "true");

        Predicate result = CouponSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).equal(activePath, Boolean.TRUE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_type_addsEnumEqualPredicate() {
        when(root.get("type")).thenReturn(typePath);
        when(cb.equal(typePath, CouponType.PERCENTAGE)).thenReturn(equalPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("type", "PERCENTAGE");

        Predicate result = CouponSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).equal(typePath, CouponType.PERCENTAGE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_search_addsLikeOnLowerCode() {
        when(root.get("code")).thenReturn(codePath);
        when(cb.lower(codePath)).thenReturn(lowerCode);
        when(cb.like(lowerCode, "%save10%")).thenReturn(likePredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("search", "SAVE10");

        Predicate result = CouponSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).like(lowerCode, "%save10%");
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_allFilters_combinesAllPredicates() {
        when(root.get("active")).thenReturn(activePath);
        when(root.get("type")).thenReturn(typePath);
        when(root.get("code")).thenReturn(codePath);
        when(cb.equal(activePath, Boolean.FALSE)).thenReturn(equalPredicate);
        when(cb.equal(typePath, CouponType.FIXED)).thenReturn(equalPredicate);
        when(cb.lower(codePath)).thenReturn(lowerCode);
        when(cb.like(lowerCode, "%black%")).thenReturn(likePredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("active", "false");
        filters.put("type", "FIXED");
        filters.put("search", "Black");

        Predicate result = CouponSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb, times(3)).and(any(Predicate.class), any(Predicate.class));
    }
}
