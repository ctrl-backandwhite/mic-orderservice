package com.backandwhite.api.controller;
import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.dto.in.ReturnRequestDtoIn;
import com.backandwhite.api.dto.in.UpdateReturnStatusDtoIn;
import com.backandwhite.api.dto.out.ReturnRequestDtoOut;
import com.backandwhite.api.mapper.ReturnApiMapper;
import com.backandwhite.api.util.PaginationMapper;
import com.backandwhite.application.usecase.ReturnUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.ReturnRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/returns")
@Tag(name = "Returns",description = "Endpointsparadevoluciones")
public class ReturnController {
private final ReturnUseCase returnUseCase;
private final ReturnApiMapper returnApiMapper;

    @PostMapping
    @Operation(summary = "Solicitardevolución",description = "Creaunasolicituddedevoluciónparaunpedidoentregado")
public ResponseEntity<ReturnRequestDtoOut> createReturn(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody ReturnRequestDtoIn dto) {
ReturnRequest request =returnApiMapper.toDomain(dto);
ReturnRequest created =returnUseCase.create(request);
return ResponseEntity.status(HttpStatus.CREATED).body(returnApiMapper.toDto(created));
    }

    @GetMapping("/me")
    @Operation(summary = "Misdevoluciones",description = "Listalasdevolucionesdelusuarioautenticado")
public ResponseEntity<PaginationDtoOut<ReturnRequestDtoOut>> getMyReturns(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader("X-Auth-Subject") String userId,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "false") boolean ascending) {
PaginationDtoOut<ReturnRequest> result =returnUseCase.findByUserId(userId,page,size,sortBy,ascending);
return ResponseEntity.ok(PaginationMapper.map(result,returnApiMapper::toDto));
    }

    // ──Admin ────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "[Admin]Listardevoluciones")
public ResponseEntity<PaginationDtoOut<ReturnRequestDtoOut>> findAll(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Filtrarporestado") @RequestParam(required =false) String status,
            @Parameter(description = "Filtrarporusuario") @RequestParam(required =false) String userId,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "false") boolean ascending) {
Map<String,Object> filters = new HashMap<>();
if (status != null)filters.put("status",status);
if (userId != null)filters.put("userId",userId);
PaginationDtoOut<ReturnRequest> result =returnUseCase.findAll(filters,page,size,sortBy,ascending);
return ResponseEntity.ok(PaginationMapper.map(result,returnApiMapper::toDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "[Admin]Detallededevolución")
public ResponseEntity<ReturnRequestDtoOut> findById(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdeladevolución") @PathVariable String id) {
ReturnRequest request =returnUseCase.findById(id);
return ResponseEntity.ok(returnApiMapper.toDto(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "[Admin]Cambiarestadodedevolución")
public ResponseEntity<ReturnRequestDtoOut> updateStatus(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdeladevolución") @PathVariable String id,
            @Valid @RequestBody UpdateReturnStatusDtoIn dto) {
ReturnRequest updated =returnUseCase.updateStatus(id,dto.getStatus());
return ResponseEntity.ok(returnApiMapper.toDto(updated));
    }
}
