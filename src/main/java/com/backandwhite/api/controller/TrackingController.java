package com.backandwhite.api.controller;
import com.backandwhite.api.dto.in.TrackingEventDtoIn;
import com.backandwhite.api.dto.out.TrackingEventDtoOut;
import com.backandwhite.api.mapper.TrackingApiMapper;
import com.backandwhite.application.usecase.TrackingUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.TrackingEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tracking")
@Tag(name = "Tracking",description = "Endpointsparatrackingdepedidos")
public class TrackingController {
private final TrackingUseCase trackingUseCase;
private final TrackingApiMapper trackingApiMapper;

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Obtenertrackingdeunpedido",description = "Devuelvetodosloseventosdetrackingdeunpedido")
public ResponseEntity<List<TrackingEventDtoOut>> getTrackingByOrder(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelpedido") @PathVariable String orderId) {
List<TrackingEvent> events =trackingUseCase.findByOrderId(orderId);
return ResponseEntity.ok(trackingApiMapper.toDtoList(events));
    }

    @PostMapping
    @Operation(summary = "[Admin]Agregareventodetracking",description = "Agregaunnuevoeventodetrackingaunpedido")
public ResponseEntity<TrackingEventDtoOut> addEvent(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody TrackingEventDtoIn dto) {
TrackingEvent event =trackingApiMapper.toDomain(dto);
TrackingEvent saved =trackingUseCase.addEvent(event);
return ResponseEntity.status(HttpStatus.CREATED).body(trackingApiMapper.toDto(saved));
    }
}
