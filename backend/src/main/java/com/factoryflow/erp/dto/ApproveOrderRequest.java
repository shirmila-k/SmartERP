package com.factoryflow.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveOrderRequest {
    /**
     * Optional adjusted lines before approval.
     */
    private List<AdjustedLine> adjustedLines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustedLine {
        private Long itemId;
        private Integer qty;
        private BigDecimal unitPrice;
    }
}
