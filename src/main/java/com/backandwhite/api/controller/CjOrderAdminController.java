package com.backandwhite.api.controller;

import com.backandwhite.api.dto.out.CjOrderDtoOut;
import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.service.CjFulfillmentPipelineService;
import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/cj-orders")
@Tag(name = "CJ Orders Admin", description = "Admin endpoints for CJ Dropshipping order management")
public class CjOrderAdminController {

    private final CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    private final CjFulfillmentPipelineService pipelineService;
    private final CjShoppingPort cjShoppingPort;
    private final CjOrderRepository cjOrderRepository;

    @NxAdmin
    @GetMapping("/{orderId}")
    @Operation(summary = "Get CJ order by internal orderId")
    public ResponseEntity<CjOrderDtoOut> getByOrderId(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @PathVariable String orderId) {
        CjOrder cjOrder = cjOrderFulfillmentUseCase.findByOrderId(orderId);
        return ResponseEntity.ok(toDto(cjOrder));
    }

    @NxAdmin
    @PostMapping("/{orderId}/sync")
    @Operation(summary = "Force sync CJ order status/tracking")
    public ResponseEntity<CjOrderDtoOut> syncStatus(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @PathVariable String orderId) {
        CjOrder synced = cjOrderFulfillmentUseCase.syncStatus(orderId);
        return ResponseEntity.ok(toDto(synced));
    }

    @NxAdmin
    @PostMapping("/{orderId}/submit")
    @Operation(summary = "Manually submit order to CJ (retry)")
    public ResponseEntity<CjOrderDtoOut> submitToCj(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @PathVariable String orderId) {
        CjOrder submitted = cjOrderFulfillmentUseCase.submitOrderToCj(orderId);
        return ResponseEntity.ok(toDto(submitted));
    }

    @NxAdmin
    @PostMapping("/{orderId}/retry-fulfillment")
    @Operation(summary = "Resume fulfillment pipeline from last recorded step (PIPELINE_FAILED / AWAITING_FUNDS)")
    public ResponseEntity<Map<String, String>> retryFulfillment(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth, @PathVariable String orderId) {
        CjOrder cjOrder = cjOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("CJ order not found for orderId: " + orderId));
        pipelineService.resumeFulfillment(cjOrder);
        return ResponseEntity.ok(Map.of("status", "pipeline resumed", "orderId", orderId));
    }

    @NxAdmin
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel a CJ order (calls CJ deleteOrder API then marks CANCELLED)")
    public ResponseEntity<Map<String, String>> cancelOrder(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @PathVariable String orderId) {
        cjOrderFulfillmentUseCase.cancelCjOrder(orderId);
        return ResponseEntity.ok(Map.of("status", "cancelled", "orderId", orderId));
    }

    @NxAdmin
    @GetMapping("/balance")
    @Operation(summary = "Query CJ account balance")
    public ResponseEntity<Map<String, String>> getBalance(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth) {
        String balance = cjShoppingPort.getBalance();
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    private CjOrderDtoOut toDto(CjOrder cjOrder) {
        return CjOrderDtoOut.builder().id(cjOrder.getId()).orderId(cjOrder.getOrderId())
                .cjOrderId(cjOrder.getCjOrderId()).shipmentOrderId(cjOrder.getShipmentOrderId())
                .cjOrderStatus(cjOrder.getCjOrderStatus()).trackNumber(cjOrder.getTrackNumber())
                .logisticName(cjOrder.getLogisticName()).productInfoList(cjOrder.getProductInfoList())
                .lastSyncedAt(cjOrder.getLastSyncedAt()).errorCount(cjOrder.getErrorCount())
                .lastError(cjOrder.getLastError()).createdAt(cjOrder.getCreatedAt()).updatedAt(cjOrder.getUpdatedAt())
                .build();
    }
}
