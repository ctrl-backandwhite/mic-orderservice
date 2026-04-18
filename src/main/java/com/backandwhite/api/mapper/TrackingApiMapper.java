package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.in.TrackingEventDtoIn;
import com.backandwhite.api.dto.out.TrackingEventDtoOut;
import com.backandwhite.domain.model.TrackingEvent;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrackingApiMapper {

    TrackingEventDtoOut toDto(TrackingEvent event);

    List<TrackingEventDtoOut> toDtoList(List<TrackingEvent> events);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    TrackingEvent toDomain(TrackingEventDtoIn dto);
}
