package com.factoryflow.erp.dto.report;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class ProductionReport {
    LocalDate startDate;
    LocalDate endDate;
    int totalRuns;
    int totalOutputPackets;
    BigDecimal totalLabourCost;
    BigDecimal totalOverheadCost;
    List<ProductionRunDetail> runs;

    @Value
    @Builder
    public static class ProductionRunDetail {
        Long runId;
        String runCode;
        LocalDate runDate;
        String productName;
        String productSku;
        BigDecimal batchesPlanned;
        Integer expectedOutputPackets;
        Integer actualGoodPackets;
        BigDecimal yieldVariancePct;
        boolean yieldFlagged;
        Integer damagedPouches;
        Integer cartonsUsed;
        BigDecimal labourCost;
        BigDecimal overheadCost;
        BigDecimal calculatedUnitCost;
        List<ConsumptionVariance> rawMaterialConsumptions;
    }

    @Value
    @Builder
    public static class ConsumptionVariance {
        String componentSku;
        String componentName;
        BigDecimal plannedQty;
        BigDecimal actualQty;
        BigDecimal variancePct;
        boolean varianceFlagged;
        BigDecimal unitCostApplied;
    }
}
