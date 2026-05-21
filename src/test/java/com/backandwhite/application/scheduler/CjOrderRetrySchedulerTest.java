package com.backandwhite.application.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.service.CjFulfillmentPipelineService;
import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjOrderRetrySchedulerTest {

    @Mock
    private CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    @Mock
    private CjFulfillmentPipelineService pipelineService;
    @Mock
    private CjOrderRepository cjOrderRepository;

    @InjectMocks
    private CjOrderRetryScheduler scheduler;

    // ── retryFailedOrders ────────────────────────────────────────────────────

    @Test
    void retryFailedOrders_emptyList_doesNothing() {
        when(cjOrderRepository.findByStatusAndErrorCountLessThan(CjOrderStatus.CREATED, 5)).thenReturn(List.of());

        scheduler.retryFailedOrders();

        verify(cjOrderFulfillmentUseCase, never()).submitOrderToCj(any());
    }

    @Test
    void retryFailedOrders_happyPath_invokesSubmitForEach() {
        CjOrder a = CjOrder.builder().orderId("o1").errorCount(0).build();
        CjOrder b = CjOrder.builder().orderId("o2").errorCount(1).build();
        when(cjOrderRepository.findByStatusAndErrorCountLessThan(CjOrderStatus.CREATED, 5)).thenReturn(List.of(a, b));

        scheduler.retryFailedOrders();

        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o1");
        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o2");
    }

    @Test
    void retryFailedOrders_whenSubmitFails_incrementsErrorAndContinues() {
        CjOrder bad = CjOrder.builder().orderId("o-bad").errorCount(2).build();
        CjOrder good = CjOrder.builder().orderId("o-good").errorCount(0).build();
        when(cjOrderRepository.findByStatusAndErrorCountLessThan(CjOrderStatus.CREATED, 5))
                .thenReturn(List.of(bad, good));
        when(cjOrderFulfillmentUseCase.submitOrderToCj("o-bad")).thenThrow(new RuntimeException("cj down"));

        scheduler.retryFailedOrders();

        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o-bad");
        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o-good");
        verify(cjOrderRepository).save(bad);
    }

    @Test
    void retryFailedOrders_saveAlsoFails_swallows() {
        CjOrder bad = CjOrder.builder().orderId("o-bad").errorCount(2).build();
        when(cjOrderRepository.findByStatusAndErrorCountLessThan(CjOrderStatus.CREATED, 5)).thenReturn(List.of(bad));
        when(cjOrderFulfillmentUseCase.submitOrderToCj("o-bad")).thenThrow(new RuntimeException("cj down"));
        when(cjOrderRepository.save(bad)).thenThrow(new RuntimeException("db down"));

        // Should not propagate.
        scheduler.retryFailedOrders();

        verify(cjOrderRepository).save(bad);
    }

    // ── retryPipelineOrders ──────────────────────────────────────────────────

    @Test
    void retryPipelineOrders_emptyList_doesNothing() {
        when(cjOrderRepository.findByStatusInAndErrorCountLessThan(anyList(), anyInt())).thenReturn(List.of());

        scheduler.retryPipelineOrders();

        verify(pipelineService, never()).resumeFulfillment(any());
    }

    @Test
    void retryPipelineOrders_happyPath_resumesEach() {
        CjOrder a = CjOrder.builder().orderId("o1").errorCount(0).build();
        CjOrder b = CjOrder.builder().orderId("o2").errorCount(1).build();
        when(cjOrderRepository.findByStatusInAndErrorCountLessThan(anyList(), anyInt())).thenReturn(List.of(a, b));

        scheduler.retryPipelineOrders();

        verify(pipelineService).resumeFulfillment(a);
        verify(pipelineService).resumeFulfillment(b);
    }

    @Test
    void retryPipelineOrders_resumeFails_incrementsAndContinues() {
        CjOrder bad = CjOrder.builder().orderId("o-bad").errorCount(2).build();
        CjOrder good = CjOrder.builder().orderId("o-good").errorCount(0).build();
        when(cjOrderRepository.findByStatusInAndErrorCountLessThan(anyList(), anyInt())).thenReturn(List.of(bad, good));
        org.mockito.Mockito.doThrow(new RuntimeException("pipeline boom")).when(pipelineService).resumeFulfillment(bad);

        scheduler.retryPipelineOrders();

        verify(pipelineService).resumeFulfillment(bad);
        verify(pipelineService).resumeFulfillment(good);
        verify(cjOrderRepository).save(bad);
    }
}
