package com.backandwhite.api.dto.in;

import com.backandwhite.domain.valureobject.ReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para actualizar el estado de una devolución")
public class UpdateReturnStatusDtoIn {

    @NotNull(message = "status es obligatorio")
    @Schema(description = "Nuevo estado de la devolución", example = "APPROVED")
    private ReturnStatus status;
}
