package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valueobject.CjOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class CjOrderDtoOut {
    private String id;
    private String orderId;
    private String cjOrderId;
    private String shipmentOrderId;
    private CjOrderStatus cjOrderStatus;
    private String trackNumber;
    private String logisticName;
    private Map<String, Object> productInfoList;
    private Instant lastSyncedAt;
    private int errorCount;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
}
