package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.infrastructure.db.postgres.entity.ShippingCarrierEntity;
import com.backandwhite.infrastructure.db.postgres.entity.ShippingRuleEntity;
import com.backandwhite.infrastructure.db.postgres.entity.TaxRuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShippingTaxInfraMapper {

    ShippingCarrier toCarrierDomain(ShippingCarrierEntity entity);

    ShippingCarrierEntity toCarrierEntity(ShippingCarrier domain);

    @Mapping(target = "carrierName", source = "carrier.name")
    ShippingRule toRuleDomain(ShippingRuleEntity entity);

    @Mapping(target = "carrier", ignore = true)
    ShippingRuleEntity toRuleEntity(ShippingRule domain);

    TaxRule toTaxDomain(TaxRuleEntity entity);

    TaxRuleEntity toTaxEntity(TaxRule domain);
}
