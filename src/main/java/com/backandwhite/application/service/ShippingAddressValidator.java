package com.backandwhite.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

/**
 * Fase 3 — sanity-checks a shipping address before we hand it to CJ. CJ's API
 * rejects orders with malformed or empty fields with cryptic errors, so we
 * catch the common problems upstream with actionable messages.
 *
 * <p>
 * Also normalises obvious issues (country-code casing, trailing spaces) so
 * downstream code can trust the shape of the data.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ShippingAddressValidator {

    private static final Pattern ZIP_GENERIC = Pattern.compile("^[A-Z0-9\\- ]{3,10}$");
    private static final int MIN_LINE_LEN = 5;
    private static final int MAX_LINE_LEN = 200;

    private final CjCountryService countryService;

    public ValidationResult validate(Address input) {
        List<String> errors = new ArrayList<>();
        Address normalised = normalise(input);

        if (isBlank(normalised.getCountryCode()) || normalised.getCountryCode().length() != 2) {
            errors.add("Country code must be the 2-letter ISO code");
        } else if (!countryService.isAllowed(normalised.getCountryCode())) {
            errors.add("CJ does not ship to " + normalised.getCountryCode());
        }

        if (isBlank(normalised.getCity())) {
            errors.add("City is required");
        }
        if (isBlank(normalised.getProvince())) {
            errors.add("State/Province is required");
        }
        if (isBlank(normalised.getLine1())) {
            errors.add("Address line 1 is required");
        } else if (normalised.getLine1().length() < MIN_LINE_LEN || normalised.getLine1().length() > MAX_LINE_LEN) {
            errors.add("Address line 1 length must be between " + MIN_LINE_LEN + " and " + MAX_LINE_LEN);
        }
        if (isBlank(normalised.getPostCode())) {
            errors.add("Postal code is required");
        } else if (!ZIP_GENERIC.matcher(normalised.getPostCode().toUpperCase(Locale.ROOT)).matches()) {
            errors.add("Postal code has an invalid format");
        }
        if (isBlank(normalised.getPhone())) {
            errors.add("Phone is required (CJ uses it for last-mile delivery)");
        }

        return new ValidationResult(errors.isEmpty(), errors, normalised);
    }

    private Address normalise(Address input) {
        return Address.builder().countryCode(safeUpper(input.getCountryCode())).province(safeTrim(input.getProvince()))
                .city(safeTrim(input.getCity())).line1(safeTrim(input.getLine1())).line2(safeTrim(input.getLine2()))
                .postCode(safeTrim(input.getPostCode())).phone(safeTrim(input.getPhone()))
                .recipient(safeTrim(input.getRecipient())).build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static String safeUpper(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    @Value
    @lombok.Builder
    public static class Address {
        String countryCode;
        String province;
        String city;
        String line1;
        String line2;
        String postCode;
        String phone;
        String recipient;
    }

    @Value
    public static class ValidationResult {
        boolean valid;
        List<String> errors;
        Address normalised;
    }
}
