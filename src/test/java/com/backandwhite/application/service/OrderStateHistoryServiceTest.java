package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.CjOrderStateHistoryEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderStateHistoryEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjOrderStateHistoryJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.OrderStateHistoryJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStateHistoryService")
class OrderStateHistoryServiceTest {

    @Mock
    private OrderStateHistoryJpaRepository orderHistoryRepository;
    @Mock
    private CjOrderStateHistoryJpaRepository cjHistoryRepository;

    @InjectMocks
    private OrderStateHistoryService service;

    @Test
    @DisplayName("recordOrderTransition persists an entity with the given fields")
    void recordOrderTransition() {
        when(orderHistoryRepository.save(any(OrderStateHistoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        OrderStateHistoryEntity result = service.recordOrderTransition("ord-1", "PENDING", "CONFIRMED",
                OrderStateHistoryService.ACTOR_SYSTEM, "system", "auto-confirm", "{}");
        assertThat(result.getOrderId()).isEqualTo("ord-1");
        assertThat(result.getFromStatus()).isEqualTo("PENDING");
        assertThat(result.getToStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getActor()).isEqualTo("SYSTEM");
        assertThat(result.getReason()).isEqualTo("auto-confirm");
        assertThat(result.getAt()).isNotNull();
    }

    @Test
    @DisplayName("recordCjTransition persists a CJ history entity")
    void recordCjTransition() {
        when(cjHistoryRepository.save(any(CjOrderStateHistoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        CjOrderStateHistoryEntity result = service.recordCjTransition("cj-1", "ord-1", "CREATED", "IN_PRODUCTION",
                OrderStateHistoryService.ACTOR_WEBHOOK, "cj webhook", null);
        assertThat(result.getCjOrderId()).isEqualTo("cj-1");
        assertThat(result.getFromStatus()).isEqualTo("CREATED");
        assertThat(result.getToStatus()).isEqualTo("IN_PRODUCTION");
        assertThat(result.getAt()).isNotNull();
    }

    @Test
    @DisplayName("findOrderHistory delegates to the order history repo")
    void findOrderHistory() {
        when(orderHistoryRepository.findAllByOrderIdOrderByAtAsc("ord")).thenReturn(List.of());
        assertThat(service.findOrderHistory("ord")).isEmpty();
    }

    @Test
    @DisplayName("findCjHistoryByOrder delegates to the CJ history repo by orderId")
    void findCjHistoryByOrder() {
        when(cjHistoryRepository.findAllByOrderIdOrderByAtAsc("ord")).thenReturn(List.of());
        assertThat(service.findCjHistoryByOrder("ord")).isEmpty();
    }

    @Test
    @DisplayName("findCjHistoryByCjOrder delegates to the CJ history repo by cjOrderId")
    void findCjHistoryByCjOrder() {
        when(cjHistoryRepository.findAllByCjOrderIdOrderByAtAsc("cj-1")).thenReturn(List.of());
        assertThat(service.findCjHistoryByCjOrder("cj-1")).isEmpty();
    }
}
