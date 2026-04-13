package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.db.postgres.mapper.CjOrderInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.CjOrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CjOrderRepositoryImpl implements CjOrderRepository {

    private final CjOrderJpaRepository jpa;
    private final CjOrderInfraMapper mapper;

    @Override
    public CjOrder save(CjOrder cjOrder) {
        return mapper.toDomain(jpa.save(mapper.toEntity(cjOrder)));
    }

    @Override
    public Optional<CjOrder> findByOrderId(String orderId) {
        return jpa.findByOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public Optional<CjOrder> findByCjOrderId(String cjOrderId) {
        return jpa.findByCjOrderId(cjOrderId).map(mapper::toDomain);
    }

    @Override
    public Optional<CjOrder> findByCjTrackNumber(String trackNumber) {
        return jpa.findByTrackNumber(trackNumber).map(mapper::toDomain);
    }

    @Override
    public List<CjOrder> findByStatusAndErrorCountLessThan(CjOrderStatus status, int maxErrors) {
        return mapper.toDomainList(jpa.findByCjOrderStatusAndErrorCountLessThan(status, maxErrors));
    }

    @Override
    public List<CjOrder> findByStatusInAndErrorCountLessThan(List<CjOrderStatus> statuses, int maxErrors) {
        return mapper.toDomainList(jpa.findByCjOrderStatusInAndErrorCountLessThan(statuses, maxErrors));
    }

    @Override
    public List<CjOrder> findPendingSync(int batchSize) {
        return mapper.toDomainList(jpa.findPendingSync(batchSize));
    }
}
