package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.OrderItemSnapshotEntity;
import com.backandwhite.infrastructure.db.postgres.repository.OrderItemSnapshotJpaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderItemSnapshotService")
class OrderItemSnapshotServiceTest {

    @Mock
    private OrderItemSnapshotJpaRepository repository;

    @InjectMocks
    private OrderItemSnapshotService service;

    @Test
    @DisplayName("save populates createdAt when missing")
    void savePopulatesCreatedAt() {
        OrderItemSnapshotEntity input = OrderItemSnapshotEntity.builder().orderId("ord-1").build();
        when(repository.save(input)).thenReturn(input);
        service.save(input);
        assertThat(input.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save preserves an existing createdAt")
    void savePreservesCreatedAt() {
        Instant ts = Instant.parse("2024-01-01T00:00:00Z");
        OrderItemSnapshotEntity input = OrderItemSnapshotEntity.builder().orderId("ord").createdAt(ts).build();
        when(repository.save(input)).thenReturn(input);
        service.save(input);
        assertThat(input.getCreatedAt()).isEqualTo(ts);
    }

    @Test
    @DisplayName("findByOrder delegates to the repository")
    void findByOrder() {
        when(repository.findAllByOrderIdOrderByIdAsc("ord-1")).thenReturn(List.of());
        assertThat(service.findByOrder("ord-1")).isEmpty();
    }
}
