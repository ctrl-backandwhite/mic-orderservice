package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.util.List;
import java.util.Optional;

public interface CjOrderRepository {
    CjOrder save(CjOrder cjOrder);

    Optional<CjOrder> findByOrderId(String orderId);

    Optional<CjOrder> findByCjOrderId(String cjOrderId);

    Optional<CjOrder> findByCjTrackNumber(String trackNumber);

    List<CjOrder> findByStatusAndErrorCountLessThan(CjOrderStatus status, int maxErrors);

    List<CjOrder> findByStatusInAndErrorCountLessThan(List<CjOrderStatus> statuses, int maxErrors);

    List<CjOrder> findPendingSync(int batchSize);
}
