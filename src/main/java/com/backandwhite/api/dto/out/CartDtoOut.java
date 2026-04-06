package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valueobject.CartStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Carrito de compras")
public class CartDtoOut {

    @Schema(description = "ID del carrito")
    private String id;

    @Schema(description = "ID del usuario")
    private String userId;

    @Schema(description = "Session ID (anónimos)")
    private String sessionId;

    @Schema(description = "Estado del carrito")
    private CartStatus status;

    @Schema(description = "Fecha de expiración")
    private Instant expiresAt;

    @Schema(description = "Items del carrito")
    private List<CartItemDtoOut> items;

    @Schema(description = "Subtotal")
    private BigDecimal subtotal;

    @Schema(description = "Cantidad de items")
    private int itemCount;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
