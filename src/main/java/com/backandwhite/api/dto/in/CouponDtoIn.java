package com.backandwhite.api.dto.in;

import com.backandwhite.domain.valueobject.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear/actualizar un cupón")
public class CouponDtoIn {

    @NotBlank(message = "code es obligatorio")
    @Schema(description = "Código del cupón", example = "WELCOME10")
    private String code;

    @NotNull(message = "type es obligatorio")
    @Schema(description = "Tipo de cupón", example = "PERCENTAGE")
    private CouponType type;

    @NotNull(message = "value es obligatorio")
    @Schema(description = "Valor del cupón", example = "10.00")
    private BigDecimal value;

    @Schema(description = "Monto mínimo del pedido", example = "50.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "Máximo de usos totales", example = "1000")
    private Integer maxUses;

    @Schema(description = "Máximo de usos por usuario", example = "1")
    private Integer maxUsesPerUser;

    @NotNull(message = "validFrom es obligatorio")
    @Schema(description = "Fecha de inicio")
    private Instant validFrom;

    @NotNull(message = "validUntil es obligatorio")
    @Schema(description = "Fecha de expiración")
    private Instant validUntil;

    @Schema(description = "Si está activo", example = "true")
    private boolean active;

    @Schema(description = "Categorías a las que aplica (null = todas)")
    private List<String> appliesToCategories;

    @Schema(description = "Productos a los que aplica (null = todos)")
    private List<String> appliesToProducts;
}
