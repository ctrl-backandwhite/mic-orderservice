package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.infrastructure.db.postgres.entity.ReturnRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReturnInfraMapper {

    @Mapping(target = "orderNumber", source = "order.orderNumber")
    ReturnRequest toDomain(ReturnRequestEntity entity);

    @Mapping(target = "order", ignore = true)
    ReturnRequestEntity toEntity(ReturnRequest domain);
}
