package com.factoryflow.erp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class SalesOrderRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "salesmanId is required")
    private Long salesmanId;

    @NotEmpty(message = "lines cannot be empty")
    @Valid
    private List<OrderLineRequest> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineRequest {
        @NotNull(message = "itemId is required")
        private Long itemId;

        @NotNull(message = "qty is required")
        @Positive(message = "qty must be greater than zero")
        private Integer qty;

        /**
         * Optional unit price override. If null, defaults to items.selling_price.
         */
        private BigDecimal unitPrice;
    }
}
