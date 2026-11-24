package com.factoryflow.erp.dto.report;

import com.factoryflow.erp.domain.enums.MovementType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class AuditStockLedgerReport {
    Long filteredItemId;
    List<StockLedgerRow> rows;

    @Value
    @Builder
    public static class StockLedgerRow {
        Long id;
        LocalDateTime createdAt;
        String itemSku;
        String itemName;
        MovementType movementType;
        BigDecimal qtyChange;
        BigDecimal unitRate;
        String sourceType;
        Long sourceId;
        String remarks;
    }
}
