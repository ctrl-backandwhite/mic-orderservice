package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.in.ReturnRequestDtoIn;
import com.backandwhite.api.dto.out.ReturnRequestDtoOut;
import com.backandwhite.domain.model.ReturnRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = MoneyMapperHelper.class)
public interface ReturnApiMapper {

    ReturnRequestDtoOut toDto(ReturnRequest request);

    List<ReturnRequestDtoOut> toDtoList(List<ReturnRequest> requests);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ReturnRequest toDomain(ReturnRequestDtoIn dto);
}
