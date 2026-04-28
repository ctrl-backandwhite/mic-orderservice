package com.backandwhite.infrastructure.client;

import com.backandwhite.application.port.out.CmsPort;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.domain.valueobject.Money;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for service-to-service calls to mic-cmsservice. Used to fetch
 * active campaigns for server-side discount validation.
 */
@Log4j2
@Component
public class CmsClient implements CmsPort {

    private final RestClient restClient;

    public CmsClient(@Value("${services.cms.url:http://localhost:6006}") String baseUrl) {
        // Inter-service calls bypass the gateway, so the gateway-issued
        // X-nx036-auth header is set by hand. NxRequestFilter on the CMS side
        // rejects /api/** without it.
        this.restClient = RestClient.builder().baseUrl(baseUrl).defaultHeader(AppConstants.HEADER_NX036_AUTH, "service")
                .build();
    }

    /**
     * Fetches all active campaigns from the CMS service.
     *
     * @return list of active campaign data maps, or empty list if the call fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActiveCampaigns() {
        try {
            List<Map<String, Object>> campaigns = restClient.get().uri("/api/v1/campaigns/active").retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return campaigns != null ? campaigns : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch active campaigns: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Calculates the campaign discount for a given product. Checks both
     * appliesToProducts and appliesToCategories. Returns the BEST (highest)
     * discount if multiple campaigns match.
     *
     * @param campaigns
     *            list of active campaign data
     * @param productId
     *            the product ID to check
     * @param categoryId
     *            the product's category ID
     * @param basePrice
     *            the product's base sell price
     * @return the discount result including amount and campaign ID
     */
    @Override
    @SuppressWarnings("unchecked")
    public CampaignDiscountResult calculateBestCampaignDiscount(List<Map<String, Object>> campaigns, String productId,
            String categoryId, Money basePrice, Money costPrice, int quantity, Money orderSubtotal) {

        Money bestDiscount = Money.zero();
        String bestCampaignId = null;

        for (Map<String, Object> campaign : campaigns) {
            boolean applies = false;

            // Check appliesToProducts
            List<String> productIds = (List<String>) campaign.get("appliesToProducts");
            if (productIds != null && productIds.contains(productId)) {
                applies = true;
            }

            // Check appliesToCategories
            if (!applies && categoryId != null) {
                List<String> categoryIds = (List<String>) campaign.get("appliesToCategories");
                if (categoryIds != null && categoryIds.contains(categoryId)) {
                    applies = true;
                }
            }

            // If neither list is set, the campaign applies to all products
            if (!applies && (productIds == null || productIds.isEmpty())
                    && ((List<String>) campaign.get("appliesToCategories") == null
                            || ((List<String>) campaign.get("appliesToCategories")).isEmpty())) {
                applies = true;
            }

            if (!applies) {
                continue;
            }

            // Check minOrder threshold (skip campaign if order subtotal is below minimum)
            Object minOrderObj = campaign.get("minOrder");
            if (minOrderObj != null) {
                BigDecimal minOrderVal = new BigDecimal(minOrderObj.toString());
                if (minOrderVal.compareTo(BigDecimal.ZERO) > 0
                        && orderSubtotal.getAmount().compareTo(minOrderVal) < 0) {
                    continue;
                }
            }

            // Calculate discount based on campaign type.
            // Discounts are applied ONLY to the profit margin (basePrice − costPrice),
            // never to the supplier cost price.
            String type = campaign.get("type") != null ? campaign.get("type").toString() : "";
            BigDecimal value = campaign.get("value") != null
                    ? new BigDecimal(campaign.get("value").toString())
                    : BigDecimal.ZERO;

            Money margin = basePrice.subtract(costPrice);
            if (margin.isNegative()) {
                margin = Money.zero(); // guard: no margin = no discount
            }

            Money discount = switch (type) {
                case "PERCENTAGE", "FLASH" -> margin.percentage(value); // pct% of margin only
                case "FIXED" -> Money.of(value).min(margin); // cap at margin
                case "BUY2GET1" -> {
                    // Buy 2 Get 1 free: groups of 3 → 1 free per group
                    int groupSize = 3;
                    if (quantity >= groupSize) {
                        int freeCount = (quantity / groupSize);
                        // Per-unit discount = (freeCount × margin) / quantity
                        BigDecimal totalOff = margin.getAmount().multiply(BigDecimal.valueOf(freeCount));
                        BigDecimal perUnit = totalOff.divide(BigDecimal.valueOf(quantity), 4,
                                java.math.RoundingMode.HALF_UP);
                        yield Money.of(perUnit).min(margin);
                    }
                    yield Money.zero();
                }
                case "BUNDLE" -> {
                    // Bundle: buy X get Y free — discount only when qty >= buyQty + getQty
                    Object buyObj = campaign.get("buyQty");
                    Object getObj = campaign.get("getQty");
                    int buyQty = buyObj != null ? Integer.parseInt(buyObj.toString()) : 0;
                    int getQty = getObj != null ? Integer.parseInt(getObj.toString()) : 0;
                    int groupSize = buyQty + getQty;
                    if (groupSize > 0 && getQty > 0 && quantity >= groupSize) {
                        int freeCount = (quantity / groupSize) * getQty;
                        BigDecimal totalOff = margin.getAmount().multiply(BigDecimal.valueOf(freeCount));
                        BigDecimal perUnit = totalOff.divide(BigDecimal.valueOf(quantity), 4,
                                java.math.RoundingMode.HALF_UP);
                        yield Money.of(perUnit).min(margin);
                    }
                    yield Money.zero();
                }
                case "FREE_SHIPPING" -> Money.zero(); // shipping discount handled separately
                default -> Money.zero();
            };

            // Apply maxDiscount cap if configured
            Object maxDiscObj = campaign.get("maxDiscount");
            if (maxDiscObj != null) {
                BigDecimal maxDiscVal = new BigDecimal(maxDiscObj.toString());
                if (maxDiscVal.compareTo(BigDecimal.ZERO) > 0) {
                    Money cap = Money.of(maxDiscVal);
                    discount = discount.min(cap);
                }
            }

            if (discount.isGreaterThan(bestDiscount)) {
                bestDiscount = discount;
                bestCampaignId = campaign.get("id") != null ? campaign.get("id").toString() : null;
            }
        }

        // Ensure discount doesn't exceed the margin (price must stay >= costPrice)
        Money maxDiscount = basePrice.subtract(costPrice);
        if (maxDiscount.isNegative())
            maxDiscount = Money.zero();
        return new CampaignDiscountResult(bestDiscount.min(maxDiscount), bestCampaignId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BigDecimal getExchangeRate(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank() || "USD".equalsIgnoreCase(currencyCode)) {
            return BigDecimal.ONE;
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/v1/currency-rates/{code}", currencyCode.toUpperCase()).retrieve().body(Map.class);
            if (response != null && response.get("rate") != null) {
                return new BigDecimal(response.get("rate").toString());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rate for {}: {}", currencyCode, e.getMessage());
        }
        return BigDecimal.ONE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isFreeShippingCampaignActive(List<Map<String, Object>> campaigns, List<String> productIds,
            List<String> categoryIds, Money orderSubtotal) {

        for (Map<String, Object> campaign : campaigns) {
            String type = campaign.get("type") != null ? campaign.get("type").toString() : "";
            if (!"FREE_SHIPPING".equals(type))
                continue;

            // Check minOrder threshold
            Object minOrderObj = campaign.get("minOrder");
            if (minOrderObj != null) {
                BigDecimal minOrderVal = new BigDecimal(minOrderObj.toString());
                if (minOrderVal.compareTo(BigDecimal.ZERO) > 0
                        && orderSubtotal.getAmount().compareTo(minOrderVal) < 0) {
                    continue;
                }
            }

            List<String> campaignProducts = (List<String>) campaign.get("appliesToProducts");
            List<String> campaignCategories = (List<String>) campaign.get("appliesToCategories");

            boolean noScope = (campaignProducts == null || campaignProducts.isEmpty())
                    && (campaignCategories == null || campaignCategories.isEmpty());

            if (noScope)
                return true; // applies to all

            // Check if ALL cart products/categories are covered
            boolean allCovered = true;
            for (String pid : productIds) {
                boolean covered = (campaignProducts != null && campaignProducts.contains(pid));
                if (!covered && categoryIds != null) {
                    for (String cid : categoryIds) {
                        if (campaignCategories != null && campaignCategories.contains(cid)) {
                            covered = true;
                            break;
                        }
                    }
                }
                if (!covered) {
                    allCovered = false;
                    break;
                }
            }
            if (allCovered)
                return true;
        }
        return false;
    }
}
