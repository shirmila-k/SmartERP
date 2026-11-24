package com.factoryflow.erp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRunRequest {

    private String runCode;

    @NotNull(message = "finishedItemId is required")
    private Long finishedItemId;

    @NotNull(message = "batchesPlanned is required")
    @DecimalMin(value = "0.01", message = "batchesPlanned must be positive")
    private BigDecimal batchesPlanned;

    @NotEmpty(message = "rawConsumptions must not be empty")
    @Valid
    private List<RawConsumptionItem> rawConsumptions;

    @NotNull(message = "actualGoodPackets is required")
    @Min(value = 1, message = "actualGoodPackets must be at least 1")
    private Integer actualGoodPackets;

    @NotNull(message = "damagedPouches is required")
    @Min(value = 0, message = "damagedPouches cannot be negative")
    private Integer damagedPouches;

    @NotNull(message = "cartonsUsed is required")
    @Min(value = 0, message = "cartonsUsed cannot be negative")
    private Integer cartonsUsed;

    @NotNull(message = "labourCost is required")
    @DecimalMin(value = "0.00", message = "labourCost cannot be negative")
    private BigDecimal labourCost;

    @NotNull(message = "overheadCost is required")
    @DecimalMin(value = "0.00", message = "overheadCost cannot be negative")
    private BigDecimal overheadCost;

    @NotNull(message = "runDate is required")
    private LocalDate runDate;

    @NotNull(message = "createdBy is required")
    private Long createdBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawConsumptionItem {
        @NotNull(message = "itemId is required")
        private Long itemId;

        @NotNull(message = "actualQty is required")
        @DecimalMin(value = "0.001", message = "actualQty must be positive")
        private BigDecimal actualQty;
    }
}
