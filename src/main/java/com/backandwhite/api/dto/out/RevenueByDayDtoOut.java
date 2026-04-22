package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

/**
 * Daily revenue bucket. Returned in chronological order by the
 * {@code /api/v1/orders/stats/revenue-by-day} endpoint.
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Revenue aggregated for a single calendar day")
public class RevenueByDayDtoOut {

    @Schema(description = "Calendar day (ISO-8601 date)", example = "2026-04-22")
    private LocalDate day;

    @Schema(description = "Gross revenue from non-cancelled / non-refunded orders")
    private BigDecimal revenue;

    @Schema(description = "Total number of orders created on this day, regardless of status")
    private long orders;

    @Schema(description = "Revenue from orders whose current status is REFUNDED")
    private BigDecimal refunded;

    @Schema(description = "Revenue from orders whose current status is CANCELLED")
    private BigDecimal cancelled;
}
