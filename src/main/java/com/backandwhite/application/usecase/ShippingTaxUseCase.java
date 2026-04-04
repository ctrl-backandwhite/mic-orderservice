package com.backandwhite.application.usecase;

import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.api.dto.PaginationDtoOut;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingTaxUseCase {

    // Shipping carriers
    ShippingCarrier createCarrier(ShippingCarrier carrier);

    ShippingCarrier updateCarrier(String id, ShippingCarrier carrier);

    ShippingCarrier findCarrierById(String id);

    PaginationDtoOut<ShippingCarrier> findAllCarriers(int page, int size, String sortBy, boolean ascending);

    void deleteCarrier(String id);

    // Shipping rules
    ShippingRule createRule(ShippingRule rule);

    ShippingRule updateRule(String id, ShippingRule rule);

    ShippingRule findRuleById(String id);

    PaginationDtoOut<ShippingRule> findAllRules(int page, int size, String sortBy, boolean ascending);

    List<ShippingRule> findShippingOptions(String country, BigDecimal weight, BigDecimal subtotal);

    void deleteRule(String id);

    // Tax rules
    TaxRule createTaxRule(TaxRule rule);

    TaxRule updateTaxRule(String id, TaxRule rule);

    TaxRule findTaxRuleById(String id);

    PaginationDtoOut<TaxRule> findAllTaxRules(int page, int size, String sortBy, boolean ascending);

    BigDecimal calculateTax(String country, String region, BigDecimal subtotal);

    void deleteTaxRule(String id);
}
