package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.infrastructure.db.postgres.entity.TrackingEventEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrackingInfraMapper {

    TrackingEvent toDomain(TrackingEventEntity entity);

    TrackingEventEntity toEntity(TrackingEvent domain);

    List<TrackingEvent> toDomainList(List<TrackingEventEntity> entities);
}
