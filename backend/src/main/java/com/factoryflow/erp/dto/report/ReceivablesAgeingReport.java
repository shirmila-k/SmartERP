package com.factoryflow.erp.dto.report;

import com.factoryflow.erp.domain.enums.PaymentMode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class ReceivablesAgeingReport {
    LocalDate asOfDate;
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal totalInvoicedAll;
    BigDecimal totalCollectedPeriod;
    BigDecimal totalOutstandingAll;
    List<PaymentCollected> paymentsCollected;
    List<CustomerAgeing> customerAgeing;

    @Value
    @Builder
    public static class PaymentCollected {
        Long paymentId;
        Long invoiceId;
        String invoiceNumber;
        String customerName;
        BigDecimal amountPaid;
        PaymentMode paymentMode;
        LocalDate paymentDate;
    }

    @Value
    @Builder
    public static class CustomerAgeing {
        Long customerId;
        String customerName;
        String gstin;
        String stateName;
        BigDecimal totalInvoiced;
        BigDecimal totalPaid;
        BigDecimal balanceDue;
        BigDecimal bucket0to30;
        BigDecimal bucket31to60;
        BigDecimal bucket60Plus;
    }
}
