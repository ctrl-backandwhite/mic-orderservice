package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.in.ShippingCarrierDtoIn;
import com.backandwhite.api.dto.in.ShippingRuleDtoIn;
import com.backandwhite.api.dto.in.TaxRuleDtoIn;
import com.backandwhite.api.dto.out.ShippingCarrierDtoOut;
import com.backandwhite.api.dto.out.ShippingRuleDtoOut;
import com.backandwhite.api.dto.out.TaxRuleDtoOut;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MoneyMapperHelper.class)
public interface ShippingTaxApiMapper {

    ShippingCarrierDtoOut toCarrierDto(ShippingCarrier carrier);

    List<ShippingCarrierDtoOut> toCarrierDtoList(List<ShippingCarrier> carriers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", expression = "java(dto.isActive())")
    ShippingCarrier toCarrierDomain(ShippingCarrierDtoIn dto);

    // Domain uses {minWeight, maxWeight, minPrice, maxPrice} but the
    // outbound DTO renames them to {weightMin, weightMax, priceMin, priceMax}
    // for the public API. Without these explicit mappings MapStruct can't
    // auto-bridge the rename, so the admin UI was getting null weight ranges
    // and falling back to 30 kg defaults — making rule edits look broken.
    @Mapping(source = "minWeight", target = "weightMin")
    @Mapping(source = "maxWeight", target = "weightMax")
    @Mapping(source = "minPrice", target = "priceMin")
    @Mapping(source = "maxPrice", target = "priceMax")
    ShippingRuleDtoOut toRuleDto(ShippingRule rule);

    // List delegate — MapStruct automatically uses toRuleDto for each element,
    // inheriting the mappings declared above.
    List<ShippingRuleDtoOut> toRuleDtoList(List<ShippingRule> rules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "carrierName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", expression = "java(dto.getActive() != null ? dto.getActive() : true)")
    @Mapping(source = "weightMin", target = "minWeight")
    @Mapping(source = "weightMax", target = "maxWeight")
    @Mapping(source = "priceMin", target = "minPrice")
    @Mapping(source = "priceMax", target = "maxPrice")
    ShippingRule toRuleDomain(ShippingRuleDtoIn dto);

    TaxRuleDtoOut toTaxRuleDto(TaxRule rule);

    List<TaxRuleDtoOut> toTaxRuleDtoList(List<TaxRule> rules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TaxRule toTaxRuleDomain(TaxRuleDtoIn dto);
}
