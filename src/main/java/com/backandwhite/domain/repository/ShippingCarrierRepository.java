package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.ShippingCarrier;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShippingCarrierRepository {
    ShippingCarrier save(ShippingCarrier carrier);

    ShippingCarrier update(ShippingCarrier carrier);

    Optional<ShippingCarrier> findById(String id);

    Page<ShippingCarrier> findAll(Map<String, Object> filters, Pageable pageable);

    void delete(String id);
}
