package com.backandwhite.infrastructure.client;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.backandwhite.application.port.out.CatalogPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for service-to-service calls to mic-productcategory.
 * Used to validate product prices and stock availability before order creation.
 */
@Log4j2
@Component
public class CatalogClient implements CatalogPort {

    private final RestClient restClient;

    public CatalogClient(
            @Value("${services.productcategory.url:http://localhost:6002}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Fetches the actual sell price and category for a product/variant from the
     * catalog.
     * If variantId is provided, returns the variant's sell price.
     * Otherwise, returns the product's base sell price.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<ProductVerification> getVerifiedPriceAndCategory(
            String productId, String variantId) {
        try {
            Map<String, Object> product = restClient.get()
                    .uri("/api/v1/products/{id}?locale=en", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (product == null) {
                return Optional.empty();
            }

            String categoryId = product.get("categoryId") != null
                    ? product.get("categoryId").toString()
                    : null;

            // If variantId provided, find matching variant and use its price
            if (variantId != null && !variantId.isBlank()) {
                List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
                if (variants != null) {
                    for (Map<String, Object> v : variants) {
                        if (variantId.equals(v.get("vid"))) {
                            Object price = v.get("variantSellPrice");
                            Object weight = v.get("variantWeight");
                            BigDecimal variantWeight = weight != null
                                    ? new BigDecimal(weight.toString())
                                    : BigDecimal.ZERO;
                            if (price != null) {
                                return Optional.of(new ProductVerification(
                                        new BigDecimal(price.toString()), categoryId, variantWeight));
                            }
                        }
                    }
                }
            }

            // Fallback to product-level price
            Object sellPrice = product.get("sellPrice");
            if (sellPrice != null && !sellPrice.toString().isBlank()) {
                return Optional.of(new ProductVerification(
                        new BigDecimal(sellPrice.toString()), categoryId, BigDecimal.ZERO));
            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to verify price for product={}, variant={}: {}",
                    productId, variantId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Checks stock availability for a variant.
     *
     * @return available stock count, or -1 if the check failed (service
     *         unavailable)
     */
    @Override
    public int getAvailableStock(String variantId) {
        try {
            Map<String, Object> result = restClient.get()
                    .uri("/api/v1/public/products/variants/{vid}/stock", variantId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (result != null && result.containsKey("available")) {
                return ((Number) result.get("available")).intValue();
            }
            return -1;
        } catch (Exception e) {
            log.warn("Failed to check stock for variant={}: {}", variantId, e.getMessage());
            return -1;
        }
    }
}
