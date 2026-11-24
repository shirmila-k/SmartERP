package com.factoryflow.erp.dto.report;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class SalesRegisterReport {
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal totalTaxableAmount;
    BigDecimal totalCgstAmount;
    BigDecimal totalSgstAmount;
    BigDecimal totalIgstAmount;
    BigDecimal totalGrandAmount;
    List<SalesRegisterEntry> entries;
    List<CustomerBreakdown> customerBreakdown;
    List<ProductBreakdown> productBreakdown;

    @Value
    @Builder
    public static class SalesRegisterEntry {
        Long invoiceId;
        String invoiceNumber;
        LocalDate invoiceDate;
        String customerName;
        String customerGstin;
        String stateName;
        BigDecimal taxableAmount;
        BigDecimal cgstAmount;
        BigDecimal sgstAmount;
        BigDecimal igstAmount;
        BigDecimal grandTotal;
        String status;
    }

    @Value
    @Builder
    public static class CustomerBreakdown {
        String customerName;
        String gstin;
        String stateName;
        int totalInvoices;
        BigDecimal totalTaxable;
        BigDecimal totalTax;
        BigDecimal totalAmount;
    }

    @Value
    @Builder
    public static class ProductBreakdown {
        String skuCode;
        String productName;
        int totalQuantity;
        BigDecimal totalTaxable;
    }
}
