package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class InvoiceResponse {
    Long id;
    String invoiceNumber;
    Long orderId;
    String orderNumber;
    Long customerId;
    String customerName;
    String customerState;
    String customerGstin;
    BigDecimal taxableAmount;
    BigDecimal cgstAmount;
    BigDecimal sgstAmount;
    BigDecimal igstAmount;
    BigDecimal grandTotal;
    BigDecimal totalPaid;
    BigDecimal balanceDue;
    InvoiceStatus status;
    LocalDate invoiceDate;
}
