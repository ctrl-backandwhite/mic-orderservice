package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.infrastructure.db.postgres.entity.ReturnRequestEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.ReturnInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.ReturnRequestJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReturnRepositoryImplTest {

    @Mock
    private ReturnRequestJpaRepository jpa;

    @Mock
    private ReturnInfraMapper mapper;

    @InjectMocks
    private ReturnRepositoryImpl adapter;

    private ReturnRequest req(String id) {
        return ReturnRequest.builder().id(id).orderId("ord-1").userId("u1").build();
    }

    @Test
    void save_assignsUuid() {
        ReturnRequest in = req(null);
        ReturnRequestEntity entity = new ReturnRequestEntity();
        ReturnRequestEntity saved = new ReturnRequestEntity();
        ReturnRequest out = req("x");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        ReturnRequest result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void update_keepsId() {
        ReturnRequest in = req("kept");
        ReturnRequestEntity entity = new ReturnRequestEntity();
        ReturnRequestEntity saved = new ReturnRequestEntity();
        ReturnRequest out = req("kept");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        assertThat(adapter.update(in)).isSameAs(out);
        assertThat(in.getId()).isEqualTo("kept");
    }

    @Test
    void findById_present() {
        ReturnRequestEntity e = new ReturnRequestEntity();
        when(jpa.findById("r1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(req("r1"));
        assertThat(adapter.findById("r1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(jpa.findById("r1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("r1")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_delegates() {
        ReturnRequestEntity e = new ReturnRequestEntity();
        Page<ReturnRequestEntity> p = new PageImpl<>(List.of(e));
        when(jpa.findAll(any(Specification.class), eq(PageRequest.of(0, 5)))).thenReturn(p);
        when(mapper.toDomain(e)).thenReturn(req("r1"));

        assertThat(adapter.findAll(Map.of("status", "REQUESTED"), PageRequest.of(0, 5)).getContent()).hasSize(1);
    }

    @Test
    void findByUserId_delegates() {
        ReturnRequestEntity e = new ReturnRequestEntity();
        Page<ReturnRequestEntity> p = new PageImpl<>(List.of(e));
        when(jpa.findByUserId("u1", PageRequest.of(0, 5))).thenReturn(p);
        when(mapper.toDomain(e)).thenReturn(req("r1"));

        assertThat(adapter.findByUserId("u1", PageRequest.of(0, 5)).getContent()).hasSize(1);
    }
}
