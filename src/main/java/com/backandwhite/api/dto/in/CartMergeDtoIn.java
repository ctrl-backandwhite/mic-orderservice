package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para fusionar carrito anónimo con usuario")
public class CartMergeDtoIn {

    @Schema(description = "Session ID del carrito anónimo", example = "sess-abc123")
    private String sessionId;
}
