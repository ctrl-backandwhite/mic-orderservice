package com.backandwhite.domain.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Intermediate result produced by each step of the CJ fulfillment pipeline
 * (addCart → addCartConfirm → generateParentOrder → payBalanceV2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CjFulfillmentResult {

    /** Whether the step succeeded. */
    private boolean success;

    /** The shipmentsId returned by addCartConfirm. */
    private String shipmentsId;

    /** The payId returned by generateParentOrder. */
    private String payId;

    /** Total amount to pay (actualPayment field from CJ). */
    private BigDecimal actualPayment;

    /** Postage portion of the payment. */
    private BigDecimal postage;

    /** Product amount portion. */
    private BigDecimal productAmount;

    /** Tax fee portion. */
    private BigDecimal taxFee;

    /** Error reason if the step failed or was intercepted by CJ. */
    private String errorReason;
}
