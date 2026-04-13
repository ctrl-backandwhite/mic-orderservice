package com.backandwhite.api.controller;

import com.backandwhite.api.dto.in.TrackingEventDtoIn;
import com.backandwhite.api.dto.out.TrackingEventDtoOut;
import com.backandwhite.api.mapper.TrackingApiMapper;
import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.usecase.TrackingUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.domain.model.CjTrackInfo;
import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.domain.repository.CjOrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tracking")
@Tag(name = "Tracking", description = "Endpoints para tracking de pedidos")
public class TrackingController {
    private final TrackingUseCase trackingUseCase;
    private final TrackingApiMapper trackingApiMapper;
    private final CjShoppingPort cjShoppingPort;
    private final CjOrderRepository cjOrderRepository;

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Obtener tracking de un pedido", description = "Devuelve todos los eventos de tracking de un pedido")
    public ResponseEntity<List<TrackingEventDtoOut>> getTrackingByOrder(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "ID del pedido") @PathVariable String orderId) {
        List<TrackingEvent> events = trackingUseCase.findByOrderId(orderId);
        return ResponseEntity.ok(trackingApiMapper.toDtoList(events));
    }

    @GetMapping("/orders/{orderId}/cj")
    @Operation(summary = "Rich CJ tracking for an order", description = "Fetches live tracking information from CJ Dropshipping API using "
            +
            "the track number stored on the CJ order.")
    public ResponseEntity<Map<String, Object>> getCjTracking(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @PathVariable String orderId) {
        return cjOrderRepository.findByOrderId(orderId)
                .map(cjOrder -> {
                    String trackNumber = cjOrder.getTrackNumber();
                    if (trackNumber == null || trackNumber.isBlank()) {
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "trackNumber", "",
                                "message", "Order has not been shipped yet"));
                    }
                    try {
                        CjTrackInfo info = cjShoppingPort.getTrackInfo(trackNumber);
                        if (info == null) {
                            return ResponseEntity.ok(Map.<String, Object>of(
                                    "trackNumber", trackNumber,
                                    "message", "No tracking data available yet"));
                        }
                        return ResponseEntity.<Map<String, Object>>ok(Map.of(
                                "trackingNumber", info.getTrackingNumber(),
                                "logisticName", nullSafe(info.getLogisticName()),
                                "trackingFrom", nullSafe(info.getTrackingFrom()),
                                "trackingTo", nullSafe(info.getTrackingTo()),
                                "trackingStatus", nullSafe(info.getTrackingStatus()),
                                "deliveryDay", nullSafe(info.getDeliveryDay()),
                                "deliveryTime", nullSafe(info.getDeliveryTime()),
                                "lastMileCarrier", nullSafe(info.getLastMileCarrier()),
                                "lastTrackNumber", nullSafe(info.getLastTrackNumber())));
                    } catch (Exception e) {
                        log.warn("CJ tracking lookup failed for orderId={}: {}", orderId, e.getMessage());
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "trackNumber", trackNumber,
                                "message", "Tracking lookup temporary unavailable"));
                    }
                })
                .orElse(ResponseEntity.ok(Map.of("message", "CJ order not found for orderId: " + orderId)));
    }

    @PostMapping
    @Operation(summary = "[Admin] Agregar evento de tracking", description = "Agrega un nuevo evento de tracking a un pedido")
    public ResponseEntity<TrackingEventDtoOut> addEvent(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody TrackingEventDtoIn dto) {
        TrackingEvent event = trackingApiMapper.toDomain(dto);
        TrackingEvent saved = trackingUseCase.addEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(trackingApiMapper.toDto(saved));
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
