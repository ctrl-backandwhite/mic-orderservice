package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.in.CouponDtoIn;
import com.backandwhite.api.dto.out.CouponDtoOut;
import com.backandwhite.domain.model.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CouponApiMapper {

    CouponDtoOut toDto(Coupon coupon);

    List<CouponDtoOut> toDtoList(List<Coupon> coupons);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Coupon toDomain(CouponDtoIn dto);
}
