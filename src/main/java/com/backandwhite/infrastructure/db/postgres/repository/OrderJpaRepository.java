package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String>, JpaSpecificationExecutor<OrderEntity> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    @Query("SELECT COUNT(o) FROM OrderEntity o")
    long countAll();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valureobject.OrderStatus.PENDING")
    long countPending();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valureobject.OrderStatus.PROCESSING")
    long countProcessing();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valureobject.OrderStatus.SHIPPED OR o.status = com.backandwhite.domain.valureobject.OrderStatus.IN_TRANSIT")
    long countShipped();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valureobject.OrderStatus.DELIVERED")
    long countDelivered();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = com.backandwhite.domain.valureobject.OrderStatus.CANCELLED")
    long countCancelled();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM OrderEntity o WHERE o.status NOT IN (com.backandwhite.domain.valureobject.OrderStatus.CANCELLED, com.backandwhite.domain.valureobject.OrderStatus.REFUNDED)")
    BigDecimal sumTotalRevenue();
}
