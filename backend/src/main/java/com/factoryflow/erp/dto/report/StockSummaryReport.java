package com.factoryflow.erp.dto.report;

import com.factoryflow.erp.domain.enums.ItemType;
import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class StockSummaryReport {
    LocalDate startDate;
    LocalDate endDate;
    List<StockSummaryItem> items;

    @Value
    @Builder
    public static class StockSummaryItem {
        Long itemId;
        String skuCode;
        String itemName;
        ItemType itemType;
        UnitOfMeasure unit;
        BigDecimal openingBalance;
        BigDecimal receipts;
        BigDecimal issuesAndSales;
        BigDecimal closingBalance;
        BigDecimal standardRate;
        BigDecimal closingStockValue;
    }
}
