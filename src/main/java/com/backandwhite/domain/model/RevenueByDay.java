package com.backandwhite.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

/**
 * Domain model for a single day's revenue bucket. Kept locale-neutral — every
 * amount is stored in the order currency as recorded at checkout; front-end
 * converts for display.
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByDay {
    private LocalDate day;
    private BigDecimal revenue;
    private long orders;
    private BigDecimal refunded;
    private BigDecimal cancelled;
}
