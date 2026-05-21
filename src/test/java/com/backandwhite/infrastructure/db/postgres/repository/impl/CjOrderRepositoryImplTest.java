package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CjOrderEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.CjOrderInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.CjOrderJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjOrderRepositoryImplTest {

    @Mock
    private CjOrderJpaRepository jpa;

    @Mock
    private CjOrderInfraMapper mapper;

    @InjectMocks
    private CjOrderRepositoryImpl adapter;

    private CjOrder cj(String id) {
        return CjOrder.builder().id(id).orderId("o-" + id).build();
    }

    @Test
    void save_delegatesThroughMapper() {
        CjOrder in = cj("1");
        CjOrderEntity entity = new CjOrderEntity();
        CjOrderEntity saved = new CjOrderEntity();
        CjOrder out = cj("1");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        assertThat(adapter.save(in)).isSameAs(out);
    }

    @Test
    void findByOrderId_present() {
        CjOrderEntity e = new CjOrderEntity();
        when(jpa.findByOrderId("o1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(cj("1"));

        assertThat(adapter.findByOrderId("o1")).isPresent();
    }

    @Test
    void findByOrderId_empty() {
        when(jpa.findByOrderId("o1")).thenReturn(Optional.empty());
        assertThat(adapter.findByOrderId("o1")).isEmpty();
    }

    @Test
    void findByCjOrderId_present() {
        CjOrderEntity e = new CjOrderEntity();
        when(jpa.findByCjOrderId("cj1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(cj("1"));

        assertThat(adapter.findByCjOrderId("cj1")).isPresent();
    }

    @Test
    void findByCjOrderId_empty() {
        when(jpa.findByCjOrderId("cj1")).thenReturn(Optional.empty());
        assertThat(adapter.findByCjOrderId("cj1")).isEmpty();
    }

    @Test
    void findByCjTrackNumber_present() {
        CjOrderEntity e = new CjOrderEntity();
        when(jpa.findByTrackNumber("trk")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(cj("1"));

        assertThat(adapter.findByCjTrackNumber("trk")).isPresent();
    }

    @Test
    void findByCjTrackNumber_empty() {
        when(jpa.findByTrackNumber("trk")).thenReturn(Optional.empty());
        assertThat(adapter.findByCjTrackNumber("trk")).isEmpty();
    }

    @Test
    void findByStatusAndErrorCountLessThan_delegates() {
        List<CjOrderEntity> entities = List.of(new CjOrderEntity());
        List<CjOrder> domain = List.of(cj("1"));
        when(jpa.findByCjOrderStatusAndErrorCountLessThan(CjOrderStatus.UNSHIPPED, 3)).thenReturn(entities);
        when(mapper.toDomainList(entities)).thenReturn(domain);

        assertThat(adapter.findByStatusAndErrorCountLessThan(CjOrderStatus.UNSHIPPED, 3)).isSameAs(domain);
    }

    @Test
    void findByStatusInAndErrorCountLessThan_delegates() {
        List<CjOrderStatus> statuses = List.of(CjOrderStatus.UNSHIPPED, CjOrderStatus.SHIPPED);
        List<CjOrderEntity> entities = List.of(new CjOrderEntity());
        List<CjOrder> domain = List.of(cj("1"));
        when(jpa.findByCjOrderStatusInAndErrorCountLessThan(statuses, 5)).thenReturn(entities);
        when(mapper.toDomainList(entities)).thenReturn(domain);

        assertThat(adapter.findByStatusInAndErrorCountLessThan(statuses, 5)).isSameAs(domain);
    }

    @Test
    void findPendingSync_delegates() {
        List<CjOrderEntity> entities = List.of(new CjOrderEntity());
        List<CjOrder> domain = List.of(cj("1"));
        when(jpa.findPendingSync(10)).thenReturn(entities);
        when(mapper.toDomainList(entities)).thenReturn(domain);

        assertThat(adapter.findPendingSync(10)).isSameAs(domain);
    }
}
