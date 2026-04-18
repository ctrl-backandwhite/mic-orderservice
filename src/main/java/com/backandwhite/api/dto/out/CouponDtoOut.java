package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valueobject.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cupón de descuento")
public class CouponDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "Código")
    private String code;

    @Schema(description = "Tipo")
    private CouponType type;

    @Schema(description = "Valor")
    private BigDecimal value;

    @Schema(description = "Monto mínimo")
    private BigDecimal minOrderAmount;

    @Schema(description = "Máximo de usos")
    private Integer maxUses;

    @Schema(description = "Usos actuales")
    private int usedCount;

    @Schema(description = "Máximo por usuario")
    private Integer maxUsesPerUser;

    @Schema(description = "Válido desde")
    private Instant validFrom;

    @Schema(description = "Válido hasta")
    private Instant validUntil;

    @Schema(description = "Activo")
    private boolean active;

    @Schema(description = "Categorías")
    private List<String> appliesToCategories;

    @Schema(description = "Productos")
    private List<String> appliesToProducts;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
