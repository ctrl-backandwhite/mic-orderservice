package com.backandwhite.application.port.out;

import com.backandwhite.common.domain.valueobject.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Port interface for CMS service interactions (campaign discount calculations).
 */
public interface CmsPort {

    /**
     * Fetches all active campaigns from the CMS service.
     */
    List<Map<String, Object>> getActiveCampaigns();

    /**
     * Calculates the best campaign discount for a given product.
     * Discounts are applied only to the profit margin (basePrice − costPrice),
     * never to the supplier cost price.
     */
    Money calculateBestCampaignDiscount(
            List<Map<String, Object>> campaigns,
            String productId,
            String categoryId,
            Money basePrice,
            Money costPrice);

    /**
     * Fetches the exchange rate for a currency code from CMS currency-rates API.
     * Returns the CurrencyLayer rate (USD → target), e.g. 0.926 for EUR.
     * Returns BigDecimal.ONE if the code is USD or if the call fails.
     */
    BigDecimal getExchangeRate(String currencyCode);
}
