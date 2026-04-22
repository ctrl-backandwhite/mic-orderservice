package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String>, JpaSpecificationExecutor<OrderEntity> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Optional<OrderEntity> findByCjOrderId(String cjOrderId);

    @Query("SELECT COUNT(o) FROM OrderEntity o")
    long countAll();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valueobject.OrderStatus.PENDING")
    long countPending();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valueobject.OrderStatus.PROCESSING")
    long countProcessing();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valueobject.OrderStatus.SHIPPED OR o.status = com.backandwhite.domain.valueobject.OrderStatus.IN_TRANSIT")
    long countShipped();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valueobject.OrderStatus.DELIVERED")
    long countDelivered();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valueobject.OrderStatus.CANCELLED")
    long countCancelled();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM OrderEntity o WHERE o.status NOT IN (com.backandwhite.domain.valueobject.OrderStatus.CANCELLED, com.backandwhite.domain.valueobject.OrderStatus.REFUNDED)")
    BigDecimal sumTotalRevenue();

    /**
     * Revenue time series grouped by calendar day. Native query (PostgreSQL
     * DATE(created_at)) so the grouping happens in the database — keeps the report
     * snappy even on large order tables.
     *
     * Returns rows as {day (java.sql.Date), grossRevenue (BigDecimal), orderCount
     * (Long), refundedRevenue (BigDecimal), cancelledRevenue (BigDecimal)}.
     * CANCELLED/REFUNDED orders are split out so the caller can render
     * revenue/returns/cancelled as three overlay series.
     */
    @Query(value = """
            SELECT DATE(o.created_at) AS day,
                   COALESCE(SUM(CASE WHEN o.status NOT IN ('CANCELLED','REFUNDED') THEN o.total ELSE 0 END), 0) AS gross,
                   COUNT(*) AS orders_count,
                   COALESCE(SUM(CASE WHEN o.status = 'REFUNDED' THEN o.total ELSE 0 END), 0) AS refunded,
                   COALESCE(SUM(CASE WHEN o.status = 'CANCELLED' THEN o.total ELSE 0 END), 0) AS cancelled
            FROM orders o
            WHERE o.created_at BETWEEN :from AND :to
            GROUP BY DATE(o.created_at)
            ORDER BY DATE(o.created_at) ASC
            """, nativeQuery = true)
    List<Object[]> findRevenueByDay(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Count of orders bucketed by status within the given window. Used to render
     * the order-status donut/distribution on the dashboard.
     */
    @Query("""
            SELECT o.status, COUNT(o)
            FROM OrderEntity o
            WHERE o.createdAt BETWEEN :from AND :to
            GROUP BY o.status
            """)
    List<Object[]> findStatusDistribution(@Param("from") Instant from, @Param("to") Instant to);
}
