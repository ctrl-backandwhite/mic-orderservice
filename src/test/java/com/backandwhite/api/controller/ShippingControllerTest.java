package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.ShippingCarrierDtoIn;
import com.backandwhite.api.dto.in.ShippingRuleDtoIn;
import com.backandwhite.api.dto.out.ShippingCarrierDtoOut;
import com.backandwhite.api.dto.out.ShippingRuleDtoOut;
import com.backandwhite.api.mapper.ShippingTaxApiMapper;
import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.common.currency.CurrencyRateCache;
import com.backandwhite.common.currency.PriceConversionService;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.CjFreightOption;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShippingControllerTest {

    @Mock
    private ShippingTaxUseCase shippingTaxUseCase;
    @Mock
    private ShippingTaxApiMapper shippingTaxApiMapper;
    @Mock
    private CurrencyRateCache currencyRateCache;
    @Mock
    private PriceConversionService priceConversionService;
    @Mock
    private CjShoppingPort cjShoppingPort;

    @InjectMocks
    private ShippingController controller;

    @Test
    void getShippingOptions_noRules_returnsDefault() {
        when(currencyRateCache.getRate(anyString())).thenReturn(BigDecimal.ONE);
        when(shippingTaxUseCase.findShippingOptions(eq("US"), any(BigDecimal.class), any(Money.class)))
                .thenReturn(List.of());
        var resp = controller.getShippingOptions("auth", "US", new BigDecimal("1.5"), new BigDecimal("100.00"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getOptions()).hasSize(1);
        assertThat(resp.getBody().getOptions().get(0).getRuleId()).isEqualTo("DEFAULT");
        assertThat(resp.getBody().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void getShippingOptions_withRules_mapsAll() {
        when(currencyRateCache.getRate(anyString())).thenReturn(BigDecimal.ONE);
        ShippingRule rule = ShippingRule.builder().id("r1").carrierName("DHL").rate(Money.of(new BigDecimal("9.99")))
                .estimatedDays(5).freeAbove(Money.of(new BigDecimal("100"))).build();
        when(shippingTaxUseCase.findShippingOptions(eq("US"), any(BigDecimal.class), any(Money.class)))
                .thenReturn(List.of(rule));
        var resp = controller.getShippingOptions("auth", "US", new BigDecimal("1"), new BigDecimal("50"));
        assertThat(resp.getBody().getOptions()).hasSize(1);
        assertThat(resp.getBody().getOptions().get(0).getRuleId()).isEqualTo("r1");
        assertThat(resp.getBody().getOptions().get(0).getRate()).isEqualByComparingTo("9.99");
    }

    @Test
    void getShippingOptions_zeroRate_isFreeShipping() {
        when(currencyRateCache.getRate(anyString())).thenReturn(BigDecimal.ONE);
        ShippingRule rule = ShippingRule.builder().id("r1").carrierName("Free").rate(Money.of(BigDecimal.ZERO))
                .estimatedDays(7).build();
        when(shippingTaxUseCase.findShippingOptions(eq("US"), any(BigDecimal.class), any(Money.class)))
                .thenReturn(List.of(rule));
        var resp = controller.getShippingOptions("auth", "US", new BigDecimal("1"), new BigDecimal("50"));
        assertThat(resp.getBody().getOptions().get(0).isFreeShipping()).isTrue();
        assertThat(resp.getBody().getOptions().get(0).getRate()).isEqualByComparingTo("0");
    }

    @Test
    void getShippingOptions_zeroRate_dividesByOne() {
        when(currencyRateCache.getRate(anyString())).thenReturn(BigDecimal.ZERO);
        when(shippingTaxUseCase.findShippingOptions(eq("US"), any(BigDecimal.class), any(Money.class)))
                .thenReturn(List.of());
        var resp = controller.getShippingOptions("auth", "US", new BigDecimal("1"), new BigDecimal("50"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void findAllCarriers_returnsOk() {
        PageResult<ShippingCarrier> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        when(shippingTaxUseCase.findAllCarriers(anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        var resp = controller.findAllCarriers("auth", 0, 20, "createdAt", true);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void findCarrierById_returnsOk() {
        ShippingCarrier carrier = ShippingCarrier.builder().id("c1").build();
        when(shippingTaxUseCase.findCarrierById("c1")).thenReturn(carrier);
        when(shippingTaxApiMapper.toCarrierDto(carrier)).thenReturn(ShippingCarrierDtoOut.builder().build());
        var resp = controller.findCarrierById("auth", "c1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void createCarrier_returnsCreated() {
        ShippingCarrierDtoIn in = ShippingCarrierDtoIn.builder().name("DHL").code("DHL").build();
        ShippingCarrier domain = ShippingCarrier.builder().build();
        ShippingCarrier created = ShippingCarrier.builder().id("c1").build();
        when(shippingTaxApiMapper.toCarrierDomain(in)).thenReturn(domain);
        when(shippingTaxUseCase.createCarrier(domain)).thenReturn(created);
        when(shippingTaxApiMapper.toCarrierDto(created)).thenReturn(ShippingCarrierDtoOut.builder().build());
        var resp = controller.createCarrier("auth", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void updateCarrier_returnsOk() {
        ShippingCarrierDtoIn in = ShippingCarrierDtoIn.builder().name("DHL").code("DHL").build();
        ShippingCarrier domain = ShippingCarrier.builder().build();
        ShippingCarrier updated = ShippingCarrier.builder().id("c1").build();
        when(shippingTaxApiMapper.toCarrierDomain(in)).thenReturn(domain);
        when(shippingTaxUseCase.updateCarrier("c1", domain)).thenReturn(updated);
        when(shippingTaxApiMapper.toCarrierDto(updated)).thenReturn(ShippingCarrierDtoOut.builder().build());
        var resp = controller.updateCarrier("auth", "c1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void deleteCarrier_returnsNoContent() {
        var resp = controller.deleteCarrier("auth", "c1");
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(shippingTaxUseCase).deleteCarrier("c1");
    }

    @Test
    void findAllRules_returnsOk() {
        PageResult<ShippingRule> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        when(shippingTaxUseCase.findAllRules(anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        var resp = controller.findAllRules("auth", 0, 20, "createdAt", true);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void findRuleById_returnsOk() {
        ShippingRule rule = ShippingRule.builder().id("r1").build();
        when(shippingTaxUseCase.findRuleById("r1")).thenReturn(rule);
        when(shippingTaxApiMapper.toRuleDto(rule)).thenReturn(ShippingRuleDtoOut.builder().build());
        var resp = controller.findRuleById("auth", "r1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void createRule_returnsCreated() {
        ShippingRuleDtoIn in = ShippingRuleDtoIn.builder().build();
        ShippingRule domain = ShippingRule.builder().build();
        ShippingRule created = ShippingRule.builder().id("r1").build();
        when(shippingTaxApiMapper.toRuleDomain(in)).thenReturn(domain);
        when(shippingTaxUseCase.createRule(domain)).thenReturn(created);
        when(shippingTaxApiMapper.toRuleDto(created)).thenReturn(ShippingRuleDtoOut.builder().build());
        var resp = controller.createRule("auth", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void updateRule_returnsOk() {
        ShippingRuleDtoIn in = ShippingRuleDtoIn.builder().build();
        ShippingRule domain = ShippingRule.builder().build();
        ShippingRule updated = ShippingRule.builder().id("r1").build();
        when(shippingTaxApiMapper.toRuleDomain(in)).thenReturn(domain);
        when(shippingTaxUseCase.updateRule("r1", domain)).thenReturn(updated);
        when(shippingTaxApiMapper.toRuleDto(updated)).thenReturn(ShippingRuleDtoOut.builder().build());
        var resp = controller.updateRule("auth", "r1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void deleteRule_returnsNoContent() {
        var resp = controller.deleteRule("auth", "r1");
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(shippingTaxUseCase).deleteRule("r1");
    }

    @Test
    void calculateFreight_returnsOk() {
        when(cjShoppingPort.calculateFreight(eq("US"), anyList())).thenReturn(List.<CjFreightOption>of());
        var resp = controller.calculateFreight("auth", "US", "vid-1", 2);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
