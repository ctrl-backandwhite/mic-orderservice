package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.repository.ShippingCarrierRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.ShippingTaxInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.ShippingCarrierJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShippingCarrierRepositoryImpl implements ShippingCarrierRepository {

    private final ShippingCarrierJpaRepository jpa;
    private final ShippingTaxInfraMapper mapper;

    @Override
    public ShippingCarrier save(ShippingCarrier carrier) {
        carrier.setId(UUID.randomUUID().toString());
        return mapper.toCarrierDomain(jpa.save(mapper.toCarrierEntity(carrier)));
    }

    @Override
    public ShippingCarrier update(ShippingCarrier carrier) {
        return mapper.toCarrierDomain(jpa.save(mapper.toCarrierEntity(carrier)));
    }

    @Override
    public Optional<ShippingCarrier> findById(String id) {
        return jpa.findById(id).map(mapper::toCarrierDomain);
    }

    @Override
    public Page<ShippingCarrier> findAll(Map<String, Object> filters, Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toCarrierDomain);
    }

    @Override
    public void delete(String id) {
        jpa.deleteById(id);
    }
}
