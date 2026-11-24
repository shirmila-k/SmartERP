package com.factoryflow.erp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {

    @NotBlank(message = "supplierName is required")
    private String supplierName;

    @NotBlank(message = "invoiceNo is required")
    private String invoiceNo;

    @NotNull(message = "purchaseDate is required")
    private LocalDate purchaseDate;

    @NotNull(message = "itemId is required")
    private Long itemId;

    @NotNull(message = "qty is required")
    @DecimalMin(value = "0.001", message = "qty must be positive")
    private BigDecimal qty;

    @NotNull(message = "unitRate is required")
    @DecimalMin(value = "0.01", message = "unitRate must be positive")
    private BigDecimal unitRate;

    @NotNull(message = "createdBy is required")
    private Long createdBy;
}
