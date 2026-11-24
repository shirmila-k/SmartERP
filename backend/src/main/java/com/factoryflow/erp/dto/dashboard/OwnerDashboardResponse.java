package com.factoryflow.erp.dto.dashboard;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class OwnerDashboardResponse {
    int todaysOrdersCount;
    BigDecimal todaysOrdersValue;
    List<FinishedGoodStockDto> finishedGoodsStock;
    List<LowStockAlertDto> lowStockAlerts;
    List<ActiveVarianceAlertDto> activeVarianceAlerts;
    LatestCostPerPacketDto latestCostPerPacket;
    BigDecimal totalOutstandingDues;

    @Value
    @Builder
    public static class FinishedGoodStockDto {
        Long itemId;
        String skuCode;
        String name;
        BigDecimal currentBalance;
        BigDecimal reorderLevel;
        boolean belowReorderLevel;
    }

    @Value
    @Builder
    public static class LowStockAlertDto {
        Long itemId;
        String skuCode;
        String name;
        String itemType;
        String unit;
        BigDecimal currentBalance;
        BigDecimal reorderLevel;
        BigDecimal shortage;
    }

    @Value
    @Builder
    public static class ActiveVarianceAlertDto {
        Long runId;
        String runCode;
        LocalDate runDate;
        String productName;
        BigDecimal yieldVariancePct;
        boolean yieldFlagged;
        List<String> flaggedConsumptions;
    }

    @Value
    @Builder
    public static class LatestCostPerPacketDto {
        Long runId;
        String runCode;
        String productName;
        BigDecimal calculatedUnitCost;
        BigDecimal sellingPrice;
        BigDecimal grossMarginPct;
    }
}
