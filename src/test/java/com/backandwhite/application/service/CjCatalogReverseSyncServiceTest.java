package com.backandwhite.application.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.backandwhite.application.port.out.OrderEventPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjCatalogReverseSyncService")
class CjCatalogReverseSyncServiceTest {

    @Mock
    private OrderEventPort orderEventPort;

    @InjectMocks
    private CjCatalogReverseSyncService service;

    @Test
    @DisplayName("forwards product update events to the event port")
    void forwardsProductUpdate() {
        service.onProductUpdate("pid-1", "{\"x\":1}");
        verify(orderEventPort).publishCatalogProductUpdate("pid-1", "{\"x\":1}");
        verifyNoMoreInteractions(orderEventPort);
    }

    @Test
    @DisplayName("forwards product delete events to the event port")
    void forwardsProductDelete() {
        service.onProductDelete("pid-2");
        verify(orderEventPort).publishCatalogProductDelete("pid-2");
        verifyNoMoreInteractions(orderEventPort);
    }

    @Test
    @DisplayName("forwards stock change events with remaining quantity")
    void forwardsStockChange() {
        service.onStockChange("vid-9", 25, "{\"a\":1}");
        verify(orderEventPort).publishCatalogStockChange("vid-9", 25, "{\"a\":1}");
        verifyNoMoreInteractions(orderEventPort);
    }
}
