package com.backandwhite.api.controller;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.dto.in.CancelOrderDtoIn;
import com.backandwhite.api.dto.in.CreateOrderDtoIn;
import com.backandwhite.api.dto.in.UpdateOrderStatusDtoIn;
import com.backandwhite.api.dto.out.OrderDtoOut;
import com.backandwhite.api.dto.out.OrderStatsDtoOut;
import com.backandwhite.api.dto.out.RevenueByDayDtoOut;
import com.backandwhite.api.dto.out.StatusCountDtoOut;
import com.backandwhite.api.mapper.OrderApiMapper;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.RevenueByDay;
import com.backandwhite.domain.model.StatusCount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Endpointsparagestióndepedidos")
public class OrderController {
    private final OrderUseCase orderUseCase;
    private final OrderApiMapper orderApiMapper;

    @NxUser
    @PostMapping
    @Operation(summary = "Crearpedido", description = "Creaunpedidoapartirdelcarritoactivodelusuario")
    public ResponseEntity<OrderDtoOut> createOrder(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader("X-Auth-Subject") String userId,
            @Parameter(description = "SessionID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody CreateOrderDtoIn dto) {
        Order order = orderUseCase.createFromCart(userId, sessionId, dto.getShippingAddress(), dto.getBillingAddress(),
                dto.getPaymentMethod(), dto.getCouponCode(), dto.getGiftCardCode(), dto.getGiftCardAmount(),
                dto.getLoyaltyPointsUsed(), dto.getLoyaltyDiscount(), dto.getNotes(), dto.getCurrencyCode(),
                dto.getCustomerLocale(), dto.getShippingRuleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderApiMapper.toDto(order));
    }

    @NxUser
    @GetMapping("/me")
    @Operation(summary = "Mispedidos", description = "Listalospedidosdelusuarioautenticado")
    public ResponseEntity<PaginationDtoOut<OrderDtoOut>> getMyOrders(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader("X-Auth-Subject") String userId,
            @Parameter(description = "Filtrarporestado") @RequestParam(required = false) String status,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "false") boolean ascending) {
        Map<String, Object> filters = new HashMap<>();
        if (status != null)
            filters.put("status", status);
        PageResult<Order> result = orderUseCase.findByUserId(userId, filters, page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, orderApiMapper::toDto));
    }

    @NxUser
    @GetMapping("/me/{id}")
    @Operation(summary = "Detalledemipedido", description = "Obtieneeldetalledeunpedidodelusuario")
    public ResponseEntity<OrderDtoOut> getMyOrder(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelpedido") @PathVariable String id) {
        return findOrderById(id);
    }

    private ResponseEntity<OrderDtoOut> findOrderById(String id) {
        Order order = orderUseCase.findById(id);
        return ResponseEntity.ok(orderApiMapper.toDto(order));
    }

    @NxUser
    @PostMapping("/me/{id}/cancel")
    @Operation(summary = "Cancelarpedido", description = "Cancelaunpedidodelusuario (solosiestáenPENDINGoCONFIRMED)")
    public ResponseEntity<OrderDtoOut> cancelOrder(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader("X-Auth-Subject") String userId,
            @Parameter(description = "IDdelpedido") @PathVariable String id,
            @RequestBody(required = false) CancelOrderDtoIn dto) {
        String reason = dto != null ? dto.getReason() : null;
        Order cancelled = orderUseCase.cancel(id, userId, reason);
        return ResponseEntity.ok(orderApiMapper.toDto(cancelled));
    }

    @NxUser
    @PostMapping("/me/{id}/confirm")
    @Operation(summary = "Confirmar pedido tras pago", description = "Transiciona orden DRAFT→PENDING, deduce stock, crea factura y envía email de factura")
    public ResponseEntity<OrderDtoOut> confirmOrder(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "ID del usuario") @RequestHeader("X-Auth-Subject") String userId,
            @Parameter(description = "Email del usuario") @RequestHeader(value = "X-Auth-Email", required = false) String email,
            @Parameter(description = "ID del pedido") @PathVariable String id) {
        Order confirmed = orderUseCase.confirmOrder(id, userId, email);
        return ResponseEntity.ok(orderApiMapper.toDto(confirmed));
    }
    // ──Adminendpoints ──────────────────────────────────────────────────

    @NxAdmin
    @GetMapping
    @Operation(summary = "[Admin]Listarpedidos", description = "Listatodoslospedidosconfiltros")
    public ResponseEntity<PaginationDtoOut<OrderDtoOut>> findAll(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Filtrarporestado") @RequestParam(required = false) String status,
            @Parameter(description = "Filtrarporusuario") @RequestParam(required = false) String userId,
            @Parameter(description = "Buscarpornúmerodepedido") @RequestParam(required = false) String search,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "false") boolean ascending) {
        Map<String, Object> filters = new HashMap<>();
        if (status != null)
            filters.put("status", status);
        if (userId != null)
            filters.put("userId", userId);
        if (search != null)
            filters.put("search", search);
        PageResult<Order> result = orderUseCase.findAll(filters, page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, orderApiMapper::toDto));
    }

    @NxAdmin
    @GetMapping("/{id}")
    @Operation(summary = "[Admin]Detalledepedido", description = "Obtieneeldetallecompletodeunpedido")
    public ResponseEntity<OrderDtoOut> getById(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelpedido") @PathVariable String id) {
        return findOrderById(id);
    }

    @NxAdmin
    @PatchMapping("/{id}/status")
    @Operation(summary = "[Admin]Cambiarestado", description = "Cambiaelestadodeunpedidosiguiendolamáquinadeestados")
    public ResponseEntity<OrderDtoOut> updateStatus(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelpedido") @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusDtoIn dto) {
        Order updated = orderUseCase.updateStatus(id, dto.getStatus(), dto.getChangedBy(), dto.getReason());
        return ResponseEntity.ok(orderApiMapper.toDto(updated));
    }

    @NxAdmin
    @GetMapping("/stats")
    @Operation(summary = "[Admin]Estadísticas", description = "Devuelveestadísticasagregadasdepedidos")
    public ResponseEntity<OrderStatsDtoOut> getStats(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth) {
        OrderStats stats = orderUseCase.getStats();
        return ResponseEntity.ok(orderApiMapper.toStatsDto(stats));
    }

    @NxAdmin
    @GetMapping("/stats/revenue-by-day")
    @Operation(summary = "[Admin] Revenue time series", description = "Returns one row per calendar day in [from, to] with gross revenue, order count, "
            + "refunded revenue and cancelled revenue. Used by Dashboard and Reports history charts. "
            + "When `from` or `to` are omitted, defaults to the last 30 days ending now.")
    public ResponseEntity<List<RevenueByDayDtoOut>> getRevenueByDay(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Window start (ISO-8601 Instant, inclusive). Defaults to now − 30 days.") @RequestParam(required = false) String from,
            @Parameter(description = "Window end (ISO-8601 Instant, inclusive). Defaults to now.") @RequestParam(required = false) String to) {
        Instant toInst = to != null ? Instant.parse(to) : Instant.now();
        Instant fromInst = from != null ? Instant.parse(from) : toInst.minus(30, ChronoUnit.DAYS);
        List<RevenueByDay> series = orderUseCase.getRevenueByDay(fromInst, toInst);
        return ResponseEntity.ok(orderApiMapper.toRevenueByDayDtoList(series));
    }

    @NxAdmin
    @GetMapping("/stats/status-distribution")
    @Operation(summary = "[Admin] Order status distribution", description = "Returns a count of orders bucketed by status inside [from, to]. Drives the "
            + "order-status donut on the dashboard. Missing window defaults to the last 30 days.")
    public ResponseEntity<List<StatusCountDtoOut>> getStatusDistribution(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant toInst = to != null ? Instant.parse(to) : Instant.now();
        Instant fromInst = from != null ? Instant.parse(from) : toInst.minus(30, ChronoUnit.DAYS);
        List<StatusCount> dist = orderUseCase.getStatusDistribution(fromInst, toInst);
        return ResponseEntity.ok(orderApiMapper.toStatusCountDtoList(dist));
    }
}
