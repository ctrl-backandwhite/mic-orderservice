package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.ShippingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ShippingRuleRepository {
    ShippingRule save(ShippingRule rule);

    ShippingRule update(ShippingRule rule);

    Optional<ShippingRule> findById(String id);

    Page<ShippingRule> findAll(Map<String, Object> filters, Pageable pageable);

    List<ShippingRule> findOptions(String country, BigDecimal weight, BigDecimal subtotal);

    void delete(String id);
}
