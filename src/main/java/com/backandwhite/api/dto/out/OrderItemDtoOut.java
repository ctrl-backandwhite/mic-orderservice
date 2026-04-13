package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item de un pedido")
public class OrderItemDtoOut {

    @Schema(description = "ID del item")
    private String id;

    @Schema(description = "ID del producto")
    private String productId;

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "Nombre del producto")
    private String productName;

    @Schema(description = "Imagen del producto")
    private String productImage;

    @Schema(description = "Cantidad")
    private int quantity;

    @Schema(description = "Precio unitario")
    private BigDecimal unitPrice;

    @Schema(description = "Precio total")
    private BigDecimal totalPrice;

    @Schema(description = "ID de campaña aplicada")
    private String campaignId;

    @Schema(description = "Descuento de campaña por unidad")
    private BigDecimal campaignDiscount;
}
