package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import com.backandwhite.infrastructure.db.postgres.entity.CouponEntity;
import com.backandwhite.infrastructure.db.postgres.entity.CouponUsageEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CouponInfraMapper {

    Coupon toDomain(CouponEntity entity);

    CouponEntity toEntity(Coupon domain);

    CouponUsage toUsageDomain(CouponUsageEntity entity);

    CouponUsageEntity toUsageEntity(CouponUsage domain);
}
