package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShippingAddressValidator")
class ShippingAddressValidatorTest {

    @Mock
    private CjCountryService countryService;

    @InjectMocks
    private ShippingAddressValidator validator;

    @Test
    @DisplayName("rejects addresses whose country is not CJ-allowed")
    void rejectsDisallowedCountry() {
        when(countryService.isAllowed("ZZ")).thenReturn(false);
        var result = validator.validate(ShippingAddressValidator.Address.builder().countryCode("zz").city("City")
                .province("State").line1("123 Something Street").postCode("12345").phone("+1234567890")
                .recipient("Jane Doe").build());
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("ZZ"));
    }

    @Test
    @DisplayName("passes on a well-formed address and uppercases the country code")
    void acceptsValidAddress() {
        when(countryService.isAllowed("MX")).thenReturn(true);
        var result = validator.validate(ShippingAddressValidator.Address.builder().countryCode("mx").city("CDMX")
                .province("CDMX").line1("Av. Reforma 222").postCode("06600").phone("+52 55 1234 5678")
                .recipient("Juan Pérez").build());
        assertThat(result.isValid()).isTrue();
        assertThat(result.getNormalised().getCountryCode()).isEqualTo("MX");
    }

    @Test
    @DisplayName("collects all validation errors, not just the first one")
    void accumulatesErrors() {
        var result = validator.validate(ShippingAddressValidator.Address.builder().countryCode("").province(" ")
                .city(" ").line1("").postCode("").phone("").build());
        assertThat(result.getErrors()).hasSizeGreaterThanOrEqualTo(5);
    }
}
