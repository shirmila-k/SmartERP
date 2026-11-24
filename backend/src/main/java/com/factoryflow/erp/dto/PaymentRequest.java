package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
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
public class PaymentRequest {

    @NotNull(message = "invoiceId is required")
    private Long invoiceId;

    @NotNull(message = "amountPaid is required")
    @DecimalMin(value = "0.01", message = "amountPaid must be positive")
    private BigDecimal amountPaid;

    @NotNull(message = "paymentMode is required")
    private PaymentMode paymentMode;

    @NotNull(message = "paymentDate is required")
    private LocalDate paymentDate;

    @NotNull(message = "recordedBy is required")
    private Long recordedBy;
}
