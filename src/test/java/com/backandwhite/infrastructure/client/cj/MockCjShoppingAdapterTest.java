package com.backandwhite.infrastructure.client.cj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.backandwhite.domain.model.CjFreightOption;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MockCjShoppingAdapter")
class MockCjShoppingAdapterTest {

    private MockCjShoppingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockCjShoppingAdapter();
    }

    @Test
    @DisplayName("createOrder returns a synthetic cjOrderId with MOCK_CJ_ prefix")
    void createOrderReturnsMockId() {
        var cj = adapter.createOrder(Order.builder().id("order-1").build());
        assertThat(cj.getCjOrderId()).startsWith("MOCK_CJ_");
        assertThat(cj.getCjOrderStatus()).isEqualTo(CjOrderStatus.UNPAID);
        assertThat(cj.getOrderId()).isEqualTo("order-1");
    }

    @Test
    @DisplayName("pipeline steps succeed deterministically")
    void pipelineStepsSucceed() {
        assertThat(adapter.addCart("cj-1").isSuccess()).isTrue();
        var confirm = adapter.addCartConfirm("cj-1");
        assertThat(confirm.isSuccess()).isTrue();
        assertThat(confirm.getShipmentsId()).startsWith("MOCK_SHIP_");
        var parent = adapter.generateParentOrder(confirm.getShipmentsId());
        assertThat(parent.isSuccess()).isTrue();
        assertThat(parent.getPayId()).startsWith("MOCK_PAY_");
        // payBalanceV2 is a no-op and must not throw
        adapter.payBalanceV2(confirm.getShipmentsId(), parent.getPayId());
    }

    @Test
    @DisplayName("balance returns a large value so AWAITING_FUNDS is never reached in mock mode")
    void balanceIsAbundant() {
        assertThat(adapter.getBalanceAmount()).isPositive();
        assertThat(adapter.getBalanceAmount().intValueExact()).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("freight always offers at least one option")
    void freightHasOptions() {
        var opts = adapter.calculateFreight("MX",
                List.of(CjFreightOption.ProductItem.builder().vid("vid-1").quantity(1).build()));
        assertThat(opts).isNotEmpty();
        assertThat(opts.get(0).getLogisticPrice()).isPositive();
    }

    @Test
    @DisplayName("getTrackInfo returns a synthetic IN_TRANSIT status")
    void trackInfoMocked() {
        var info = adapter.getTrackInfo("TRK-123");
        assertThat(info.getTrackingStatus()).isEqualTo("IN_TRANSIT");
    }

    @Test
    @DisplayName("deleteOrder and registerWebhookUrl are no-ops — must not throw")
    void noOpsDoNotThrow() {
        assertThatCode(() -> {
            adapter.deleteOrder("cj-x");
            adapter.registerWebhookUrl("https://example.test");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getOrderDetail returns a synthetic UNSHIPPED order with the queried id")
    void getOrderDetailMocked() {
        var detail = adapter.getOrderDetail("cj-1");
        assertThat(detail.getCjOrderId()).isEqualTo("cj-1");
        assertThat(detail.getCjOrderStatus()).isEqualTo(CjOrderStatus.UNSHIPPED);
        assertThat(detail.getTrackNumber()).startsWith("MOCK_TRK_");
    }

    @Test
    @DisplayName("getBalance returns a non-blank synthetic value")
    void getBalanceMocked() {
        assertThat(adapter.getBalance()).isNotBlank();
    }

    @Test
    @DisplayName("getRegisteredWebhookUrl returns a deterministic mock URL")
    void getRegisteredWebhookMocked() {
        assertThat(adapter.getRegisteredWebhookUrl()).contains("mock");
    }
}
