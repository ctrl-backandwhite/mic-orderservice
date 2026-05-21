package com.backandwhite.infrastructure.db.postgres.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.valueobject.OrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
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
class OrderSpecificationTest {

    @Mock
    private Root<OrderEntity> root;

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
    private Predicate orPredicate;

    @Mock
    private Predicate combined;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path statusPath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path userIdPath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path orderNumberPath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Expression lowerOrderNumber;

    @Mock
    @SuppressWarnings("rawtypes")
    private Expression lowerUserId;

    @BeforeEach
    void setUp() {
        lenient().when(cb.conjunction()).thenReturn(basePredicate);
        lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(combined);
    }

    @Test
    void withFilters_emptyMap_returnsConjunctionOnly() {
        Specification<OrderEntity> spec = OrderSpecification.withFilters(new HashMap<>());

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(basePredicate);
        verify(cb).conjunction();
        verify(cb, never()).and(any(Predicate.class), any(Predicate.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_status_addsEnumEqualPredicate() {
        when(root.get("status")).thenReturn(statusPath);
        when(cb.equal(statusPath, OrderStatus.PENDING)).thenReturn(equalPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "PENDING");

        Predicate result = OrderSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).equal(statusPath, OrderStatus.PENDING);
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_userId_addsEqualPredicate() {
        when(root.get("userId")).thenReturn(userIdPath);
        when(cb.equal(userIdPath, "user-1")).thenReturn(equalPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("userId", "user-1");

        Predicate result = OrderSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).equal(userIdPath, "user-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_search_addsOrLikePredicate() {
        when(root.get("orderNumber")).thenReturn(orderNumberPath);
        when(root.get("userId")).thenReturn(userIdPath);
        when(cb.lower(orderNumberPath)).thenReturn(lowerOrderNumber);
        when(cb.lower(userIdPath)).thenReturn(lowerUserId);
        when(cb.like(lowerOrderNumber, "%abc%")).thenReturn(likePredicate);
        when(cb.like(lowerUserId, "%abc%")).thenReturn(likePredicate);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(orPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("search", "ABC");

        Predicate result = OrderSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb).or(any(Predicate.class), any(Predicate.class));
        verify(cb).like(lowerOrderNumber, "%abc%");
        verify(cb).like(lowerUserId, "%abc%");
    }

    @Test
    @SuppressWarnings("unchecked")
    void withFilters_allFilters_combinesAllPredicates() {
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("userId")).thenReturn(userIdPath);
        when(root.get("orderNumber")).thenReturn(orderNumberPath);
        when(cb.equal(statusPath, OrderStatus.SHIPPED)).thenReturn(equalPredicate);
        when(cb.equal(userIdPath, "uX")).thenReturn(equalPredicate);
        when(cb.lower(orderNumberPath)).thenReturn(lowerOrderNumber);
        when(cb.lower(userIdPath)).thenReturn(lowerUserId);
        when(cb.like(lowerOrderNumber, "%foo%")).thenReturn(likePredicate);
        when(cb.like(lowerUserId, "%foo%")).thenReturn(likePredicate);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(orPredicate);

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "SHIPPED");
        filters.put("userId", "uX");
        filters.put("search", "foo");

        Predicate result = OrderSpecification.withFilters(filters).toPredicate(root, query, cb);

        assertThat(result).isSameAs(combined);
        verify(cb, atLeast(3)).and(any(Predicate.class), any(Predicate.class));
    }
}
