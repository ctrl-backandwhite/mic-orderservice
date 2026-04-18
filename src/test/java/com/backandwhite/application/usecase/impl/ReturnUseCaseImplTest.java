package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.exception.BusinessException;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.repository.ReturnRepository;
import com.backandwhite.domain.valueobject.OrderStatus;
import com.backandwhite.domain.valueobject.ReturnStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReturnUseCaseImplTest {

    @Mock
    private ReturnRepository returnRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPort orderEventPort;

    @InjectMocks
    private ReturnUseCaseImpl useCase;

    private Order deliveredOrder() {
        return Order.builder().id("o1").orderNumber("NX-20250101-1").status(OrderStatus.DELIVERED).userId("u1")
                .updatedAt(Instant.now().minus(2, ChronoUnit.DAYS)).total(Money.of(new BigDecimal("50.00"))).build();
    }

    @Test
    void create_happyPath_savesAndPublishesEvent() {
        ReturnRequest request = ReturnRequest.builder().orderId("o1").reason("Broken").build();
        Order order = deliveredOrder();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> {
            ReturnRequest rr = inv.getArgument(0);
            rr.setId("r1");
            return rr;
        });

        ReturnRequest saved = useCase.create(request);

        assertThat(saved.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
        assertThat(saved.getUserId()).isEqualTo("u1");
        verify(orderEventPort).publishOrderReturnRequested(eq("o1"), eq("r1"), eq("u1"), eq(null), eq("NX-20250101-1"),
                eq("Broken"));
    }

    @Test
    void create_missingOrder_throws() {
        ReturnRequest request = ReturnRequest.builder().orderId("x").build();
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.create(request)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_orderNotDelivered_throws() {
        ReturnRequest request = ReturnRequest.builder().orderId("o1").build();
        Order order = Order.builder().id("o1").status(OrderStatus.PROCESSING).updatedAt(Instant.now()).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.create(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void create_returnWindowExpired_throws() {
        ReturnRequest request = ReturnRequest.builder().orderId("o1").build();
        Order order = Order.builder().id("o1").status(OrderStatus.DELIVERED)
                .updatedAt(Instant.now().minus(60, ChronoUnit.DAYS)).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.create(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void findById_existing_returns() {
        ReturnRequest rr = ReturnRequest.builder().id("r1").build();
        when(returnRepository.findById("r1")).thenReturn(Optional.of(rr));
        assertThat(useCase.findById("r1")).isSameAs(rr);
    }

    @Test
    void findById_missing_throws() {
        when(returnRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findById("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAll_ascending() {
        Page<ReturnRequest> page = new PageImpl<>(List.of(ReturnRequest.builder().id("r1").build()));
        when(returnRepository.findAll(any(), any(Pageable.class))).thenReturn(page);

        var result = useCase.findAll(Map.of(), 0, 10, "createdAt", true);
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void findAll_descending() {
        Page<ReturnRequest> page = new PageImpl<>(List.of());
        when(returnRepository.findAll(any(), any(Pageable.class))).thenReturn(page);

        var result = useCase.findAll(Map.of(), 0, 10, "createdAt", false);
        assertThat(result.content()).isEmpty();
    }

    @Test
    void findByUserId_ascending() {
        Page<ReturnRequest> page = new PageImpl<>(List.of());
        when(returnRepository.findByUserId(eq("u1"), any(Pageable.class))).thenReturn(page);
        var result = useCase.findByUserId("u1", 0, 10, "createdAt", true);
        assertThat(result).isNotNull();
    }

    @Test
    void findByUserId_descending() {
        Page<ReturnRequest> page = new PageImpl<>(List.of());
        when(returnRepository.findByUserId(eq("u1"), any(Pageable.class))).thenReturn(page);
        var result = useCase.findByUserId("u1", 0, 10, "createdAt", false);
        assertThat(result).isNotNull();
    }

    @Test
    void updateStatus_approved_publishesEventWithRefundAmount() {
        ReturnRequest existing = ReturnRequest.builder().id("r1").orderId("o1").userId("u1")
                .refundAmount(Money.of(new BigDecimal("25.00"))).build();
        when(returnRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(returnRepository.update(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById("o1"))
                .thenReturn(Optional.of(Order.builder().id("o1").orderNumber("NX-1").build()));

        ReturnRequest updated = useCase.updateStatus("r1", ReturnStatus.APPROVED);

        assertThat(updated.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        ArgumentCaptor<String> amountCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderEventPort).publishOrderReturnApproved(eq("o1"), eq("r1"), eq("u1"), eq(null), eq("NX-1"),
                amountCaptor.capture());
        assertThat(amountCaptor.getValue()).isEqualTo("25.00");
    }

    @Test
    void updateStatus_approved_fallsBackToOrderTotalWhenNoRefund() {
        ReturnRequest existing = ReturnRequest.builder().id("r1").orderId("o1").userId("u1").build();
        Order order = Order.builder().id("o1").orderNumber("NX-1").total(Money.of(new BigDecimal("100.00"))).build();
        when(returnRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(returnRepository.update(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        useCase.updateStatus("r1", ReturnStatus.APPROVED);

        verify(orderEventPort).publishOrderReturnApproved(anyString(), anyString(), anyString(), eq(null), anyString(),
                eq("100.00"));
    }

    @Test
    void updateStatus_approved_noOrderFound_usesZero() {
        ReturnRequest existing = ReturnRequest.builder().id("r1").orderId("o1").userId("u1").build();
        when(returnRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(returnRepository.update(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById("o1")).thenReturn(Optional.empty());

        useCase.updateStatus("r1", ReturnStatus.APPROVED);

        verify(orderEventPort).publishOrderReturnApproved(eq("o1"), eq("r1"), eq("u1"), eq(null), eq(null), eq("0.00"));
    }

    @Test
    void updateStatus_rejected_doesNotPublishApproval() {
        ReturnRequest existing = ReturnRequest.builder().id("r1").orderId("o1").userId("u1").build();
        when(returnRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(returnRepository.update(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.updateStatus("r1", ReturnStatus.REJECTED);

        verify(orderEventPort, never()).publishOrderReturnApproved(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    void updateStatus_missing_throws() {
        when(returnRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.updateStatus("x", ReturnStatus.APPROVED))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
