package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CjOrderEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CjOrderInfraMapperTest {

    private CjOrderInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new CjOrderInfraMapperImpl();
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        CjOrderEntity entity = CjOrderEntity.builder().id("c1").orderId("o1").cjOrderId("CJ1")
                .cjOrderStatus(CjOrderStatus.UNSHIPPED).trackNumber("TR1").logisticName("CJ Logistics")
                .productInfoList(Map.of("a", "b")).build();
        CjOrder d = mapper.toDomain(entity);
        assertThat(d.getId()).isEqualTo("c1");
        assertThat(d.getProductInfoList()).containsEntry("a", "b");
    }

    @Test
    void toDomain_nullProductInfo() {
        CjOrderEntity entity = CjOrderEntity.builder().id("c1").productInfoList(null).build();
        CjOrder d = mapper.toDomain(entity);
        assertThat(d.getProductInfoList()).isNull();
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_full() {
        CjOrder d = CjOrder.builder().id("c1").orderId("o1").cjOrderId("CJ1").cjOrderStatus(CjOrderStatus.UNSHIPPED)
                .productInfoList(Map.of("a", "b")).build();
        CjOrderEntity e = mapper.toEntity(d);
        assertThat(e.getId()).isEqualTo("c1");
        assertThat(e.getProductInfoList()).containsEntry("a", "b");
    }

    @Test
    void toEntity_nullProductInfo() {
        CjOrder d = CjOrder.builder().id("c1").productInfoList(null).build();
        CjOrderEntity e = mapper.toEntity(d);
        assertThat(e.getProductInfoList()).isNull();
    }

    @Test
    void toDomainList_null() {
        assertThat(mapper.toDomainList(null)).isNull();
    }

    @Test
    void toDomainList_mapsList() {
        assertThat(mapper.toDomainList(List.of(CjOrderEntity.builder().id("c1").build()))).hasSize(1);
    }
}
