package com.backandwhite.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Port interface for catalog service interactions (product price verification
 * and stock checks).
 */
public interface CatalogPort {

    /**
     * Product verification info returned by the catalog.
     */
    record ProductVerification(BigDecimal price, String categoryId, BigDecimal weight) {
    }

    /**
     * Fetches the actual sell price and category for a product/variant from the
     * catalog.
     */
    Optional<ProductVerification> getVerifiedPriceAndCategory(String productId, String variantId);

    /**
     * Checks stock availability for a variant.
     *
     * @return available stock count, or -1 if the check failed
     */
    int getAvailableStock(String variantId);
}
