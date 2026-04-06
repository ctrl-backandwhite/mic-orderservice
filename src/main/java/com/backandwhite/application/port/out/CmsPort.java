package com.backandwhite.application.port.out;

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
     */
    BigDecimal calculateBestCampaignDiscount(
            List<Map<String, Object>> campaigns,
            String productId,
            String categoryId,
            BigDecimal basePrice);
}
