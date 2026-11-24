package com.factoryflow.erp.dto.report;

import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class PurchaseRegisterReport {
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal grandTotalSpend;
    List<PurchaseEntry> entries;

    @Value
    @Builder
    public static class PurchaseEntry {
        Long purchaseId;
        LocalDate purchaseDate;
        String supplierName;
        String invoiceNo;
        String itemName;
        String skuCode;
        UnitOfMeasure unit;
        BigDecimal qty;
        BigDecimal unitRate;
        BigDecimal totalAmount;
    }
}
