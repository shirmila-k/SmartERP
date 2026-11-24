package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ProductionRunResponse {
    Long id;
    String runCode;
    Long finishedItemId;
    String finishedItemSku;
    String finishedItemName;
    BigDecimal batchesPlanned;
    Integer expectedOutputPackets;
    Integer actualGoodPackets;
    Integer damagedPouches;
    Integer cartonsUsed;
    BigDecimal labourCost;
    BigDecimal overheadCost;
    BigDecimal totalRawMaterialCost;
    BigDecimal totalPackagingCost;
    BigDecimal totalBatchCost;
    BigDecimal yieldVariancePct;
    boolean yieldFlagged;
    BigDecimal calculatedUnitCost;
    LocalDate runDate;
    Long createdById;
    String createdByUsername;
    LocalDateTime createdAt;
    List<ConsumptionLineDto> consumptions;

    @Value
    @Builder
    public static class ConsumptionLineDto {
        Long id;
        Long itemId;
        String skuCode;
        String itemName;
        UnitOfMeasure unit;
        BigDecimal plannedQty;
        BigDecimal actualQty;
        BigDecimal variancePct;
        boolean varianceFlagged;
        BigDecimal unitCostApplied;
        BigDecimal totalFifoCost;
    }
}
