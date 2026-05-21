package com.backandwhite.api.controller;

import com.backandwhite.application.service.OrderItemSnapshotService;
import com.backandwhite.application.service.OrderStateHistoryService;
import com.backandwhite.application.service.TrackingUrlSigner;
import com.backandwhite.application.usecase.TrackingUseCase;
import com.backandwhite.common.security.annotation.NxPublic;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 9 — consolidated tracking endpoint exposed to the customer. Returns the
 * full timeline (state transitions + tracking events), product snapshots and
 * current status in a single call so the React /orders/{id}/tracking page can
 * render a vertical Amazon-style timeline without chaining three requests.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Customer Order Tracking", description = "Consolidated customer tracking endpoint")
public class CustomerOrderTrackingController {

    private final OrderRepository orderRepository;
    private final CjOrderRepository cjOrderRepository;
    private final TrackingUseCase trackingUseCase;
    private final OrderStateHistoryService stateHistoryService;
    private final OrderItemSnapshotService snapshotService;
    private final TrackingUrlSigner trackingUrlSigner;

    @NxUser
    @GetMapping("/{orderId}/tracking")
    @Operation(summary = "Single-call tracking view for the customer UI")
    public ResponseEntity<Map<String, Object>> tracking(@PathVariable String orderId) {
        return ResponseEntity.ok(buildTrackingPayload(orderId));
    }

    /**
     * Public HMAC-signed variant. The frontend /tracking page detects
     * {@code ?exp=&sig=} query params (linked from confirmation and shipment
     * emails) and calls this endpoint with nxFetch — no auth required. Returns 404
     * (instead of 401/403) on any failure so the endpoint is invisible to probes.
     */
    @NxPublic
    @GetMapping("/public/tracking/{orderId}")
    @Operation(summary = "Public tracking view validated by HMAC signature", description = "Accepts a link signed by TrackingUrlSigner (exp + sig query params). "
            + "Used by the storefront /tracking page for guests arriving from an email.")
    public ResponseEntity<Map<String, Object>> publicTracking(@PathVariable String orderId,
            @Parameter(description = "Expiration timestamp in Unix seconds") @RequestParam long exp,
            @Parameter(description = "HMAC-SHA256 signature over orderId|exp") @RequestParam String sig) {
        if (!trackingUrlSigner.verify(orderId, exp, sig)) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(buildTrackingPayload(orderId));
        } catch (IllegalArgumentException _) {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> buildTrackingPayload(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        var cjOrder = cjOrderRepository.findByOrderId(orderId).orElse(null);

        return Map.of("orderId", orderId, "orderStatus", order.getStatus() != null ? order.getStatus().name() : "",
                "cjOrderId", cjOrder != null ? cjOrder.getCjOrderId() : "", "cjStatus",
                cjOrder != null && cjOrder.getCjOrderStatus() != null ? cjOrder.getCjOrderStatus().name() : "",
                "trackNumber", cjOrder != null && cjOrder.getTrackNumber() != null ? cjOrder.getTrackNumber() : "",
                "events", trackingUseCase.findByOrderId(orderId), "stateHistory",
                stateHistoryService.findOrderHistory(orderId), "snapshot", snapshotService.findByOrder(orderId));
    }
}
