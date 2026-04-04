package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item del carrito")
public class CartItemDtoOut {

    @Schema(description = "ID del item")
    private String id;

    @Schema(description = "ID del producto")
    private String productId;

    @Schema(description = "ID de la variante")
    private String variantId;

    @Schema(description = "Cantidad")
    private int quantity;

    @Schema(description = "Precio unitario")
    private BigDecimal unitPrice;

    @Schema(description = "Nombre del producto")
    private String productName;

    @Schema(description = "Imagen del producto")
    private String productImage;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;
}
