package com.backandwhite.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a freight/shipping option returned by CJ's freightCalculate API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CjFreightOption {

    /**
     * CJ logistic channel name — use this value in createOrderV3's logisticName
     * field.
     */
    private String logisticName;

    /** Shipping price in USD. */
    private BigDecimal logisticPrice;

    /** Shipping price in CNY. */
    private BigDecimal logisticPriceCn;

    /** Estimated delivery days, e.g. "5-10". */
    private String logisticAging;

    /** Additional taxes fee (if any). */
    private BigDecimal taxesFee;

    /** Clearance/operation fee (if any). */
    private BigDecimal clearanceOperationFee;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductItem {
        private String vid;
        private Integer quantity;
    }
}
