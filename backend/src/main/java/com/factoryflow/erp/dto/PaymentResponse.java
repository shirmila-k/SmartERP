package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.PaymentMode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class PaymentResponse {
    Long id;
    Long invoiceId;
    String invoiceNumber;
    BigDecimal amountPaid;
    PaymentMode paymentMode;
    LocalDate paymentDate;
    Long recordedById;
    String recordedByUsername;
    BigDecimal invoiceGrandTotal;
    BigDecimal invoiceTotalPaid;
    BigDecimal invoiceBalanceDue;
}
