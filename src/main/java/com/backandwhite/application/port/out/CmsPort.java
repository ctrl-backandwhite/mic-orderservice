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
     * Result of a campaign discount calculation, including the applied campaign ID.
     */
    record CampaignDiscountResult(Money discount, String campaignId) {
        public static CampaignDiscountResult none() {
            return new CampaignDiscountResult(Money.zero(), null);
        }
    }

    /**
     * Fetches all active campaigns from the CMS service.
     */
    List<Map<String, Object>> getActiveCampaigns();

    /**
     * Calculates the best campaign discount for a given product. Discounts are
     * applied only to the profit margin (basePrice − costPrice), never to the
     * supplier cost price. Quantity is used for BUNDLE/BUY2GET1 calculations.
     * orderSubtotal is used to filter campaigns by minOrder threshold.
     */
    CampaignDiscountResult calculateBestCampaignDiscount(List<Map<String, Object>> campaigns, String productId,
            String categoryId, Money basePrice, Money costPrice, int quantity, Money orderSubtotal);

    /**
     * Fetches the exchange rate for a currency code from CMS currency-rates API.
     * Returns the CurrencyLayer rate (USD → target), e.g. 0.926 for EUR. Returns
     * BigDecimal.ONE if the code is USD or if the call fails.
     */
    BigDecimal getExchangeRate(String currencyCode);

    /**
     * Checks if any active FREE_SHIPPING campaign applies to the given
     * product/category list. Also verifies minOrder threshold against
     * orderSubtotal.
     */
    boolean isFreeShippingCampaignActive(List<Map<String, Object>> campaigns, List<String> productIds,
            List<String> categoryIds, Money orderSubtotal);
}
