package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.in.InvoiceDtoIn;
import com.backandwhite.api.dto.out.InvoiceDtoOut;
import com.backandwhite.domain.model.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceApiMapper {

    InvoiceDtoOut toDto(Invoice invoice);

    List<InvoiceDtoOut> toDtoList(List<Invoice> invoices);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Invoice toDomain(InvoiceDtoIn dto);
}
