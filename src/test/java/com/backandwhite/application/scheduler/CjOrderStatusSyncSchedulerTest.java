package com.backandwhite.application.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CjOrderStatusSyncSchedulerTest {

    @Mock
    private CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    @Mock
    private CjOrderRepository cjOrderRepository;

    @InjectMocks
    private CjOrderStatusSyncScheduler scheduler;

    @BeforeEach
    void setBatchSize() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
    }

    @Test
    void syncPendingOrders_emptyList_doesNothing() {
        when(cjOrderRepository.findPendingSync(20)).thenReturn(List.of());

        scheduler.syncPendingOrders();

        verify(cjOrderFulfillmentUseCase, never()).syncStatus(any());
    }

    @Test
    void syncPendingOrders_recentWebhook_skipsSync() {
        CjOrder fresh = CjOrder.builder().orderId("o1").errorCount(0)
                .lastWebhookAt(Instant.now().minus(5, ChronoUnit.MINUTES)).build();
        when(cjOrderRepository.findPendingSync(20)).thenReturn(List.of(fresh));

        scheduler.syncPendingOrders();

        verify(cjOrderFulfillmentUseCase, never()).syncStatus(any());
    }

    @Test
    void syncPendingOrders_oldWebhook_syncs() {
        CjOrder stale = CjOrder.builder().orderId("o1").errorCount(0)
                .lastWebhookAt(Instant.now().minus(2, ChronoUnit.HOURS)).build();
        when(cjOrderRepository.findPendingSync(20)).thenReturn(List.of(stale));

        scheduler.syncPendingOrders();

        verify(cjOrderFulfillmentUseCase).syncStatus("o1");
    }

    @Test
    void syncPendingOrders_nullWebhook_syncs() {
        CjOrder neverWebhooked = CjOrder.builder().orderId("o1").errorCount(0).lastWebhookAt(null).build();
        when(cjOrderRepository.findPendingSync(20)).thenReturn(List.of(neverWebhooked));

        scheduler.syncPendingOrders();

        verify(cjOrderFulfillmentUseCase).syncStatus("o1");
    }

    @Test
    void syncPendingOrders_syncFails_incrementsErrorAndContinues() {
        CjOrder bad = CjOrder.builder().orderId("o-bad").errorCount(2).lastWebhookAt(null).build();
        CjOrder good = CjOrder.builder().orderId("o-good").errorCount(0).lastWebhookAt(null).build();
        when(cjOrderRepository.findPendingSync(20)).thenReturn(List.of(bad, good));
        when(cjOrderFulfillmentUseCase.syncStatus("o-bad")).thenThrow(new RuntimeException("cj down"));

        scheduler.syncPendingOrders();

        verify(cjOrderFulfillmentUseCase).syncStatus("o-bad");
        verify(cjOrderFulfillmentUseCase).syncStatus("o-good");
        verify(cjOrderRepository).save(bad);
    }

    @Test
    void syncPendingOrders_saveAlsoFails_swallows() {
        CjOrder bad = CjOrder.builder().orderId("o-bad").errorCount(2).lastWebhookAt(null).build();
        when(cjOrderRepository.findPendingSync(20)).thenReturn(List.of(bad));
        when(cjOrderFulfillmentUseCase.syncStatus("o-bad")).thenThrow(new RuntimeException("cj down"));
        when(cjOrderRepository.save(bad)).thenThrow(new RuntimeException("db down"));

        scheduler.syncPendingOrders();

        verify(cjOrderRepository).save(bad);
    }

    @Test
    void syncPendingOrders_mixedSkipAndSync() {
        CjOrder fresh = CjOrder.builder().orderId("o-fresh").errorCount(0)
                .lastWebhookAt(Instant.now().minus(1, ChronoUnit.MINUTES)).build();
        CjOrder stale = CjOrder.builder().orderId("o-stale").errorCount(0).lastWebhookAt(null).build();
        when(cjOrderRepository.findPendingSync(anyInt())).thenReturn(List.of(fresh, stale));

        scheduler.syncPendingOrders();

        verify(cjOrderFulfillmentUseCase, never()).syncStatus("o-fresh");
        verify(cjOrderFulfillmentUseCase).syncStatus("o-stale");
    }
}
