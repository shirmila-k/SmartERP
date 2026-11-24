package com.factoryflow.erp.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CustomerOutstandingResponse {
    Long customerId;
    String customerName;
    String gstin;
    String stateName;
    BigDecimal totalInvoiced;
    BigDecimal totalReceived;
    BigDecimal totalOutstanding;
    List<InvoiceSummaryDto> invoices;

    @Value
    @Builder
    public static class InvoiceSummaryDto {
        Long invoiceId;
        String invoiceNumber;
        BigDecimal grandTotal;
        BigDecimal totalPaid;
        BigDecimal balanceDue;
        String status;
    }
}
