package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para agregar o actualizar un item del carrito")
public class CartItemDtoIn {

    @NotBlank(message = "productId es obligatorio")
    @Schema(description = "ID del producto", example = "prod-001")
    private String productId;

    @Schema(description = "ID de la variante (opcional)", example = "var-001")
    private String variantId;

    @NotNull(message = "quantity es obligatorio")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Schema(description = "Cantidad", example = "2")
    private Integer quantity;

    @NotNull(message = "unitPrice es obligatorio")
    @Schema(description = "Precio unitario", example = "29.99")
    private BigDecimal unitPrice;

    @Schema(description = "Nombre del producto", example = "Camiseta algodón")
    private String productName;

    @Schema(description = "Imagen del producto")
    private String productImage;

    @Schema(description = "Atributos de variante seleccionados", example = "{\"Color\":\"Rojo\",\"Talla\":\"M\"}")
    private Map<String, String> selectedAttrs;
}
