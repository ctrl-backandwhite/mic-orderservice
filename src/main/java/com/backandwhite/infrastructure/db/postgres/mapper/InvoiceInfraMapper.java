package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.Invoice;
import com.backandwhite.infrastructure.db.postgres.entity.InvoiceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceInfraMapper {

    @Mapping(target = "orderNumber", source = "order.orderNumber")
    Invoice toDomain(InvoiceEntity entity);

    @Mapping(target = "order", ignore = true)
    InvoiceEntity toEntity(Invoice domain);
}
