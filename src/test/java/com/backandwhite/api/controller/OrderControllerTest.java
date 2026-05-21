package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.CancelOrderDtoIn;
import com.backandwhite.api.dto.in.CreateOrderDtoIn;
import com.backandwhite.api.dto.in.UpdateOrderStatusDtoIn;
import com.backandwhite.api.dto.out.OrderDtoOut;
import com.backandwhite.api.dto.out.OrderStatsDtoOut;
import com.backandwhite.api.dto.out.RevenueByDayDtoOut;
import com.backandwhite.api.dto.out.StatusCountDtoOut;
import com.backandwhite.api.mapper.OrderApiMapper;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.RevenueByDay;
import com.backandwhite.domain.model.StatusCount;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderUseCase orderUseCase;
    @Mock
    private OrderApiMapper orderApiMapper;

    @InjectMocks
    private OrderController controller;

    @Test
    void createOrder_returnsCreated() {
        Order order = Order.builder().id("o1").build();
        OrderDtoOut dto = OrderDtoOut.builder().id("o1").build();
        CreateOrderDtoIn in = CreateOrderDtoIn.builder().shippingAddress(Map.of("a", "b"))
                .billingAddress(Map.of("c", "d")).paymentMethod("CARD").couponCode("C").giftCardCode("G")
                .giftCardAmount(new BigDecimal("5")).loyaltyPointsUsed(10).loyaltyDiscount(new BigDecimal("1"))
                .currencyCode("USD").notes("n").customerLocale("es").build();
        when(orderUseCase.createFromCart(eq("u1"), eq("s1"), anyMap(), anyMap(), eq("CARD"), eq("C"), eq("G"),
                any(BigDecimal.class), eq(10), any(BigDecimal.class), eq("n"), eq("USD"), eq("es"), null))
                .thenReturn(order);
        when(orderApiMapper.toDto(order)).thenReturn(dto);

        var resp = controller.createOrder("auth", "u1", "s1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void getMyOrders_buildsFilterAndPaginates() {
        PageResult<Order> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        when(orderUseCase.findByUserId(eq("u1"), anyMap(), eq(0), eq(20), eq("createdAt"), eq(false))).thenReturn(pr);
        var resp = controller.getMyOrders("auth", "u1", "PAID", 0, 20, "createdAt", false);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getContent()).isEmpty();
    }

    @Test
    void getMyOrders_nullStatus_skipsFilter() {
        PageResult<Order> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> filters = ArgumentCaptor.forClass(Map.class);
        when(orderUseCase.findByUserId(eq("u1"), filters.capture(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(pr);
        controller.getMyOrders("auth", "u1", null, 0, 20, "createdAt", false);
        assertThat(filters.getValue()).isEmpty();
    }

    @Test
    void getMyOrder_returnsOk() {
        Order order = Order.builder().id("o1").build();
        OrderDtoOut dto = OrderDtoOut.builder().id("o1").build();
        when(orderUseCase.findById("o1")).thenReturn(order);
        when(orderApiMapper.toDto(order)).thenReturn(dto);
        var resp = controller.getMyOrder("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void cancelOrder_withDto_passesReason() {
        Order order = Order.builder().id("o1").build();
        OrderDtoOut dto = OrderDtoOut.builder().id("o1").build();
        when(orderUseCase.cancel("o1", "u1", "reason")).thenReturn(order);
        when(orderApiMapper.toDto(order)).thenReturn(dto);
        var resp = controller.cancelOrder("auth", "u1", "o1", CancelOrderDtoIn.builder().reason("reason").build());
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void cancelOrder_nullDto_passesNullReason() {
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.cancel("o1", "u1", null)).thenReturn(order);
        when(orderApiMapper.toDto(order)).thenReturn(OrderDtoOut.builder().build());
        controller.cancelOrder("auth", "u1", "o1", null);
        verify(orderUseCase).cancel("o1", "u1", null);
    }

    @Test
    void confirmOrder_returnsOk() {
        Order order = Order.builder().id("o1").build();
        OrderDtoOut dto = OrderDtoOut.builder().build();
        when(orderUseCase.confirmOrder("o1", "u1", "e@x.com")).thenReturn(order);
        when(orderApiMapper.toDto(order)).thenReturn(dto);
        var resp = controller.confirmOrder("auth", "u1", "e@x.com", "o1");
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void findAll_buildsAllFilters() {
        PageResult<Order> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> filters = ArgumentCaptor.forClass(Map.class);
        when(orderUseCase.findAll(filters.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", "PAID", "u1", "search", 0, 20, "createdAt", false);
        assertThat(filters.getValue()).containsEntry("status", "PAID").containsEntry("userId", "u1")
                .containsEntry("search", "search");
    }

    @Test
    void findAll_nullParams_emptyFilters() {
        PageResult<Order> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> filters = ArgumentCaptor.forClass(Map.class);
        when(orderUseCase.findAll(filters.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", null, null, null, 0, 20, "createdAt", false);
        assertThat(filters.getValue()).isEmpty();
    }

    @Test
    void getById_admin_returnsOk() {
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.findById("o1")).thenReturn(order);
        when(orderApiMapper.toDto(order)).thenReturn(OrderDtoOut.builder().build());
        var resp = controller.getById("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void updateStatus_returnsOk() {
        UpdateOrderStatusDtoIn dto = UpdateOrderStatusDtoIn.builder().status(OrderStatus.CONFIRMED).changedBy("admin")
                .reason("ok").build();
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.updateStatus("o1", OrderStatus.CONFIRMED, "admin", "ok")).thenReturn(order);
        when(orderApiMapper.toDto(order)).thenReturn(OrderDtoOut.builder().build());
        var resp = controller.updateStatus("auth", "o1", dto);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getStats_returnsOk() {
        OrderStats stats = OrderStats.builder().build();
        when(orderUseCase.getStats()).thenReturn(stats);
        OrderStatsDtoOut dto = OrderStatsDtoOut.builder().build();
        when(orderApiMapper.toStatsDto(stats)).thenReturn(dto);
        var resp = controller.getStats("auth");
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void getRevenueByDay_withWindow_parsesIso() {
        when(orderUseCase.getRevenueByDay(any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(orderApiMapper.toRevenueByDayDtoList(any())).thenReturn(List.<RevenueByDayDtoOut>of());
        var resp = controller.getRevenueByDay("auth", "2024-01-01T00:00:00Z", "2024-01-31T00:00:00Z");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getRevenueByDay_defaultsTo30days() {
        when(orderUseCase.getRevenueByDay(any(Instant.class), any(Instant.class))).thenReturn(List.<RevenueByDay>of());
        when(orderApiMapper.toRevenueByDayDtoList(any())).thenReturn(List.<RevenueByDayDtoOut>of());
        var resp = controller.getRevenueByDay("auth", null, null);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getStatusDistribution_withWindow() {
        when(orderUseCase.getStatusDistribution(any(Instant.class), any(Instant.class)))
                .thenReturn(List.<StatusCount>of());
        when(orderApiMapper.toStatusCountDtoList(any())).thenReturn(List.<StatusCountDtoOut>of());
        var resp = controller.getStatusDistribution("auth", "2024-01-01T00:00:00Z", "2024-01-31T00:00:00Z");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getStatusDistribution_defaults() {
        when(orderUseCase.getStatusDistribution(any(Instant.class), any(Instant.class)))
                .thenReturn(List.<StatusCount>of());
        when(orderApiMapper.toStatusCountDtoList(any())).thenReturn(List.<StatusCountDtoOut>of());
        var resp = controller.getStatusDistribution("auth", null, null);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void createOrder_propagatesUseCaseException() {
        CreateOrderDtoIn in = CreateOrderDtoIn.builder().paymentMethod("CARD").build();
        when(orderUseCase.createFromCart(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), null)).thenThrow(new IllegalStateException("boom"));
        assertThatThrownBy(() -> controller.createOrder("auth", "u1", "s1", in))
                .isInstanceOf(IllegalStateException.class);
    }
}
