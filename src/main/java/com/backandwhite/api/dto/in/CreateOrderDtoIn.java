package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear un pedido desde el carrito")
public class CreateOrderDtoIn {

    @NotNull(message = "shippingAddress es obligatoria")
    @Schema(description = "Dirección de envío")
    private Map<String, Object> shippingAddress;

    @Schema(description = "Dirección de facturación (por defecto = shippingAddress)")
    private Map<String, Object> billingAddress;

    @NotBlank(message = "paymentMethod es obligatorio")
    @Schema(description = "Método de pago", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Código de cupón", example = "WELCOME10")
    private String couponCode;

    @Schema(description = "Código de tarjeta de regalo")
    private String giftCardCode;

    @Schema(description = "Monto cubierto por tarjeta de regalo", example = "12.08")
    private java.math.BigDecimal giftCardAmount;

    @Schema(description = "Puntos de lealtad canjeados", example = "500")
    private Integer loyaltyPointsUsed;

    @Schema(description = "Descuento monetario por puntos de lealtad", example = "5.00")
    private java.math.BigDecimal loyaltyDiscount;

    @Schema(description = "Código de moneda (ISO 4217)", example = "USD")
    private String currencyCode;

    @Schema(description = "Notas del pedido")
    private String notes;
}
