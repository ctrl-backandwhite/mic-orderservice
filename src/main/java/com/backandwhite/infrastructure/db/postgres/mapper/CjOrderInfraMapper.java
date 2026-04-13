package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.infrastructure.db.postgres.entity.CjOrderEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CjOrderInfraMapper {
    CjOrder toDomain(CjOrderEntity entity);

    CjOrderEntity toEntity(CjOrder domain);

    List<CjOrder> toDomainList(List<CjOrderEntity> entities);
}
