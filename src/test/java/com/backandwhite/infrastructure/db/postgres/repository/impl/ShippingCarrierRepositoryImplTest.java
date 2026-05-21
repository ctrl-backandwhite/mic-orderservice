package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.infrastructure.db.postgres.entity.ShippingCarrierEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.ShippingTaxInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.ShippingCarrierJpaRepository;
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

@ExtendWith(MockitoExtension.class)
class ShippingCarrierRepositoryImplTest {

    @Mock
    private ShippingCarrierJpaRepository jpa;

    @Mock
    private ShippingTaxInfraMapper mapper;

    @InjectMocks
    private ShippingCarrierRepositoryImpl adapter;

    private ShippingCarrier carrier(String id) {
        return ShippingCarrier.builder().id(id).code("DHL").name("DHL").build();
    }

    @Test
    void save_assignsUuid() {
        ShippingCarrier in = carrier(null);
        ShippingCarrierEntity entity = new ShippingCarrierEntity();
        ShippingCarrierEntity saved = new ShippingCarrierEntity();
        ShippingCarrier out = carrier("x");

        when(mapper.toCarrierEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toCarrierDomain(saved)).thenReturn(out);

        ShippingCarrier result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void update_keepsId() {
        ShippingCarrier in = carrier("kept");
        ShippingCarrierEntity entity = new ShippingCarrierEntity();
        ShippingCarrierEntity saved = new ShippingCarrierEntity();
        ShippingCarrier out = carrier("kept");

        when(mapper.toCarrierEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toCarrierDomain(saved)).thenReturn(out);

        assertThat(adapter.update(in)).isSameAs(out);
        assertThat(in.getId()).isEqualTo("kept");
    }

    @Test
    void findById_present() {
        ShippingCarrierEntity e = new ShippingCarrierEntity();
        when(jpa.findById("c1")).thenReturn(Optional.of(e));
        when(mapper.toCarrierDomain(e)).thenReturn(carrier("c1"));
        assertThat(adapter.findById("c1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(jpa.findById("c1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("c1")).isEmpty();
    }

    @Test
    void findAll_delegates() {
        ShippingCarrierEntity e = new ShippingCarrierEntity();
        Page<ShippingCarrierEntity> p = new PageImpl<>(List.of(e));
        when(jpa.findAll(PageRequest.of(0, 5))).thenReturn(p);
        when(mapper.toCarrierDomain(e)).thenReturn(carrier("c1"));

        assertThat(adapter.findAll(Map.of(), PageRequest.of(0, 5)).getContent()).hasSize(1);
    }

    @Test
    void delete_delegates() {
        adapter.delete("c1");
        verify(jpa).deleteById("c1");
    }
}
