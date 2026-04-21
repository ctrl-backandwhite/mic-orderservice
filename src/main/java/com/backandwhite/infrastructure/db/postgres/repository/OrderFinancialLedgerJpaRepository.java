package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.OrderFinancialLedgerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderFinancialLedgerJpaRepository extends JpaRepository<OrderFinancialLedgerEntity, Long> {

    List<OrderFinancialLedgerEntity> findAllByOrderIdOrderByCreatedAtAsc(String orderId);
}
