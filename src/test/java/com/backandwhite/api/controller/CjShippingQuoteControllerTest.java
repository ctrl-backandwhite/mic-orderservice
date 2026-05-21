package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.service.CjCountryService;
import com.backandwhite.domain.model.CjFreightOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjShippingQuoteControllerTest {

    @Mock
    private CjShoppingPort cjShoppingPort;
    @Mock
    private CjCountryService countryService;

    @InjectMocks
    private CjShippingQuoteController controller;

    @Test
    void quote_nullRequest_returnsBadRequest() {
        var resp = controller.quote(null);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void quote_nullDestination_returnsBadRequest() {
        var req = new CjShippingQuoteController.QuoteRequest(
                List.of(new CjShippingQuoteController.ProductLine("vid", 1)), null);
        var resp = controller.quote(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void quote_emptyProducts_returnsBadRequest() {
        var req = new CjShippingQuoteController.QuoteRequest(List.of(),
                new CjShippingQuoteController.Destination("US", null, null, null));
        var resp = controller.quote(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void quote_nullProducts_returnsBadRequest() {
        var req = new CjShippingQuoteController.QuoteRequest(null,
                new CjShippingQuoteController.Destination("US", null, null, null));
        var resp = controller.quote(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void quote_disallowedCountry_returns422() {
        when(countryService.isAllowed("XX")).thenReturn(false);
        var req = new CjShippingQuoteController.QuoteRequest(
                List.of(new CjShippingQuoteController.ProductLine("vid", 1)),
                new CjShippingQuoteController.Destination("XX", null, null, null));
        var resp = controller.quote(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void quote_allowed_returnsOptions() {
        when(countryService.isAllowed("US")).thenReturn(true);
        when(cjShoppingPort.calculateFreight(eq("US"), anyList())).thenReturn(List.<CjFreightOption>of());
        var req = new CjShippingQuoteController.QuoteRequest(
                List.of(new CjShippingQuoteController.ProductLine("vid-1", 2),
                        new CjShippingQuoteController.ProductLine("vid-2", 3)),
                new CjShippingQuoteController.Destination("US", null, null, null));
        var resp = controller.quote(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void fetchFreight_delegatesToPort() {
        when(cjShoppingPort.calculateFreight(eq("US"), anyList())).thenReturn(List.<CjFreightOption>of());
        var result = controller.fetchFreight("US", "key", List.of());
        assertThat(result).isEmpty();
    }
}
