package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.repository.ReturnRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.ReturnInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.ReturnRequestJpaRepository;
import com.backandwhite.infrastructure.db.postgres.specification.ReturnRequestSpecification;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnRepositoryImpl implements ReturnRepository {

    private final ReturnRequestJpaRepository jpa;
    private final ReturnInfraMapper mapper;

    @Override
    public ReturnRequest save(ReturnRequest request) {
        request.setId(UUID.randomUUID().toString());
        return mapper.toDomain(jpa.save(mapper.toEntity(request)));
    }

    @Override
    public ReturnRequest update(ReturnRequest request) {
        return mapper.toDomain(jpa.save(mapper.toEntity(request)));
    }

    @Override
    public Optional<ReturnRequest> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<ReturnRequest> findAll(Map<String, Object> filters, Pageable pageable) {
        return jpa.findAll(ReturnRequestSpecification.withFilters(filters), pageable).map(mapper::toDomain);
    }

    @Override
    public Page<ReturnRequest> findByUserId(String userId, Pageable pageable) {
        return jpa.findByUserId(userId, pageable).map(mapper::toDomain);
    }
}
