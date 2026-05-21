package com.backandwhite.infrastructure.db.postgres.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.valueobject.ReturnStatus;
import com.backandwhite.infrastructure.db.postgres.entity.ReturnRequestEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
class ReturnRequestSpecificationTest {

    @Mock
    private Root<ReturnRequestEntity> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Predicate basePredicate;

    @Mock
    private Predicate equalPredicate;

    @Mock
    private Predicate combined;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path statusPath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path userIdPath;

    @BeforeEach
    void setUp() {
        lenient().when(cb.conjunction()).thenReturn(basePredicate);
        lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(combined);
    }

    @Test
    void withFilters_emptyMap_returnsConjunctionOnly() {
        Specification<ReturnRequestEntity> spec = ReturnRequestSpecification.withFilters(new HashMap<>());

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(basePredicate);
        verify(cb).conjunction();
        verify(cb, never()).and(any(Predicate.class), any(Predicate.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_status_addsEnumEqualPredicate() {
        when(root.get("status")).thenReturn(statusPath);
        when(cb.equal(statusPath, ReturnStatus.APPROVED)).thenReturn(equalPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "APPROVED");

        Predicate result = ReturnRequestSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).equal(statusPath, ReturnStatus.APPROVED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_userId_addsEqualPredicate() {
        when(root.get("userId")).thenReturn(userIdPath);
        when(cb.equal(userIdPath, "user-7")).thenReturn(equalPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("userId", "user-7");

        Predicate result = ReturnRequestSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).equal(userIdPath, "user-7");
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_allFilters_combinesPredicates() {
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("userId")).thenReturn(userIdPath);
        when(cb.equal(statusPath, ReturnStatus.REQUESTED)).thenReturn(equalPredicate);
        when(cb.equal(userIdPath, "u1")).thenReturn(equalPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "REQUESTED");
        filters.put("userId", "u1");

        Predicate result = ReturnRequestSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb, times(2)).and(any(Predicate.class), any(Predicate.class));
    }
}
