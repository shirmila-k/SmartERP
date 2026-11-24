package com.factoryflow.erp.dto.report;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class GstSummaryReport {
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal totalTaxable;
    BigDecimal totalCgst;
    BigDecimal totalSgst;
    BigDecimal totalIgst;
    BigDecimal totalTax;
    List<B2bVsB2cSummary> b2bVsB2c;
    List<PlaceOfSupplySummary> placeOfSupplySummary;
    List<HsnSummary> hsnSummary;

    @Value
    @Builder
    public static class B2bVsB2cSummary {
        String category; // "B2B" (registered with GSTIN) or "B2C" (consumer/unregistered)
        int invoiceCount;
        BigDecimal taxableAmount;
        BigDecimal cgstAmount;
        BigDecimal sgstAmount;
        BigDecimal igstAmount;
        BigDecimal totalTax;
        BigDecimal grandTotal;
    }

    @Value
    @Builder
    public static class PlaceOfSupplySummary {
        String stateName;
        String supplyType; // "INTRA_STATE" or "INTER_STATE"
        int invoiceCount;
        BigDecimal taxableAmount;
        BigDecimal cgstAmount;
        BigDecimal sgstAmount;
        BigDecimal igstAmount;
        BigDecimal totalTax;
    }

    @Value
    @Builder
    public static class HsnSummary {
        String hsnCode;
        String description;
        int totalQuantity;
        BigDecimal taxableValue;
        BigDecimal cgstAmount;
        BigDecimal sgstAmount;
        BigDecimal igstAmount;
        BigDecimal totalTax;
    }
}
