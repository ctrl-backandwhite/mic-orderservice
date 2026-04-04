package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

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

    @Schema(description = "Notas del pedido")
    private String notes;
}
