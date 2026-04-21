package com.backandwhite.api.controller;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.service.CjCountryService;
import com.backandwhite.common.security.annotation.NxPublic;
import com.backandwhite.domain.model.CjFreightOption;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 4 — expose CJ's real freight calculator to the checkout. Requires a
 * valid destination country (validated against the {@code cj_allowed_countries}
 * whitelist) and a list of variants + quantities. Response is cached per
 * {@code (country, variant-set)} for 60 minutes in the shared Caffeine cache.
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout Shipping Quote", description = "Real-time CJ freight options at checkout")
public class CjShippingQuoteController {

    private final CjShoppingPort cjShoppingPort;
    private final CjCountryService countryService;

    @NxPublic
    @PostMapping("/shipping-quote")
    @Operation(summary = "Return real CJ freight options for a cart + destination")
    public ResponseEntity<List<CjFreightOption>> quote(@RequestBody QuoteRequest req) {
        if (req == null || req.destination() == null || req.products() == null || req.products().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String country = req.destination().countryCode();
        if (!countryService.isAllowed(country)) {
            log.info("::> Shipping quote rejected — destination {} not allowed", country);
            return ResponseEntity.status(422).build();
        }

        List<CjFreightOption> opts = fetchFreight(country, cacheKey(req.products()), toProductItems(req.products()));
        return ResponseEntity.ok(opts);
    }

    @Cacheable(cacheNames = "cjFreight", key = "#country + ':' + #cacheKey")
    public List<CjFreightOption> fetchFreight(String country, String cacheKey,
            List<CjFreightOption.ProductItem> items) {
        return cjShoppingPort.calculateFreight(country, items);
    }

    private static String cacheKey(List<ProductLine> products) {
        return products.stream().map(p -> p.vid() + "x" + p.quantity()).collect(Collectors.joining("|"));
    }

    private static List<CjFreightOption.ProductItem> toProductItems(List<ProductLine> products) {
        return products.stream()
                .map(p -> CjFreightOption.ProductItem.builder().vid(p.vid()).quantity(p.quantity()).build()).toList();
    }

    public record Destination(String countryCode, String province, String city, String postCode) {
    }

    public record ProductLine(String vid, Integer quantity) {
    }

    public record QuoteRequest(List<ProductLine> products, Destination destination) {
    }
}
