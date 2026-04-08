package com.backandwhite.infrastructure.client;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.backandwhite.application.port.out.CmsPort;
import com.backandwhite.common.domain.valueobject.Money;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for service-to-service calls to mic-cmsservice.
 * Used to fetch active campaigns for server-side discount validation.
 */
@Log4j2
@Component
public class CmsClient implements CmsPort {

    private final RestClient restClient;

    public CmsClient(
            @Value("${services.cmsservice.url:http://localhost:6006}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
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
            List<Map<String, Object>> campaigns = restClient.get()
                    .uri("/api/v1/campaigns/active")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return campaigns != null ? campaigns : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch active campaigns: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Calculates the campaign discount for a given product.
     * Checks both appliesToProducts and appliesToCategories.
     * Returns the BEST (highest) discount if multiple campaigns match.
     *
     * @param campaigns  list of active campaign data
     * @param productId  the product ID to check
     * @param categoryId the product's category ID
     * @param basePrice  the product's base sell price
     * @return the discount amount to subtract from the base price
     */
    @Override
    @SuppressWarnings("unchecked")
    public Money calculateBestCampaignDiscount(
            List<Map<String, Object>> campaigns,
            String productId,
            String categoryId,
            Money basePrice) {

        Money bestDiscount = Money.zero();

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

            // Calculate discount based on campaign type
            String type = campaign.get("type") != null ? campaign.get("type").toString() : "";
            BigDecimal value = campaign.get("value") != null
                    ? new BigDecimal(campaign.get("value").toString())
                    : BigDecimal.ZERO;

            Money discount = switch (type) {
                case "PERCENTAGE", "FLASH" -> basePrice.percentage(value);
                case "FIXED" -> Money.of(value);
                default -> Money.zero();
            };

            if (discount.isGreaterThan(bestDiscount)) {
                bestDiscount = discount;
            }
        }

        // Ensure discount doesn't exceed price
        return bestDiscount.min(basePrice);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BigDecimal getExchangeRate(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank() || "USD".equalsIgnoreCase(currencyCode)) {
            return BigDecimal.ONE;
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/v1/currency-rates/{code}", currencyCode.toUpperCase())
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("rate") != null) {
                return new BigDecimal(response.get("rate").toString());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rate for {}: {}", currencyCode, e.getMessage());
        }
        return BigDecimal.ONE;
    }
}
