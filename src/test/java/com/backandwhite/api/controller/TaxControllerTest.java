package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.TaxRuleDtoIn;
import com.backandwhite.api.dto.out.TaxRuleDtoOut;
import com.backandwhite.api.mapper.ShippingTaxApiMapper;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.TaxRule;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxControllerTest {

    @Mock
    private ShippingTaxUseCase shippingTaxUseCase;
    @Mock
    private ShippingTaxApiMapper shippingTaxApiMapper;

    @InjectMocks
    private TaxController controller;

    @Test
    void calculateTax_returnsOk() {
        when(shippingTaxUseCase.calculateTax(eq("US"), eq("CA"), any(Money.class)))
                .thenReturn(Money.of(new BigDecimal("8.00")));
        var resp = controller.calculateTax("auth", "US", "CA", new BigDecimal("100.00"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getTaxAmount()).isEqualByComparingTo("8.00");
        assertThat(resp.getBody().getTotalWithTax()).isEqualByComparingTo("108.00");
    }

    @Test
    void findAll_returnsOk() {
        PageResult<TaxRule> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        when(shippingTaxUseCase.findAllTaxRules(anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        var resp = controller.findAll("auth", 0, 20, "createdAt", true);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void findById_returnsOk() {
        TaxRule rule = TaxRule.builder().id("t1").build();
        when(shippingTaxUseCase.findTaxRuleById("t1")).thenReturn(rule);
        when(shippingTaxApiMapper.toTaxRuleDto(rule)).thenReturn(TaxRuleDtoOut.builder().id("t1").build());
        var resp = controller.findById("auth", "t1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void create_returnsCreated() {
        TaxRuleDtoIn in = TaxRuleDtoIn.builder().build();
        TaxRule domain = TaxRule.builder().build();
        TaxRule created = TaxRule.builder().id("t1").build();
        when(shippingTaxApiMapper.toTaxRuleDomain(in)).thenReturn(domain);
        when(shippingTaxUseCase.createTaxRule(domain)).thenReturn(created);
        when(shippingTaxApiMapper.toTaxRuleDto(created)).thenReturn(TaxRuleDtoOut.builder().build());
        var resp = controller.create("auth", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void update_returnsOk() {
        TaxRuleDtoIn in = TaxRuleDtoIn.builder().build();
        TaxRule domain = TaxRule.builder().build();
        TaxRule updated = TaxRule.builder().id("t1").build();
        when(shippingTaxApiMapper.toTaxRuleDomain(in)).thenReturn(domain);
        when(shippingTaxUseCase.updateTaxRule("t1", domain)).thenReturn(updated);
        when(shippingTaxApiMapper.toTaxRuleDto(updated)).thenReturn(TaxRuleDtoOut.builder().build());
        var resp = controller.update("auth", "t1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void delete_returnsNoContent() {
        var resp = controller.delete("auth", "t1");
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(shippingTaxUseCase).deleteTaxRule("t1");
    }
}
