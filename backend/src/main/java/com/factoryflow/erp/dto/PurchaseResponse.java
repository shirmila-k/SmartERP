package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class PurchaseResponse {
    Long id;
    String supplierName;
    String invoiceNo;
    LocalDate purchaseDate;
    Long itemId;
    String skuCode;
    String itemName;
    UnitOfMeasure unit;
    BigDecimal qty;
    BigDecimal unitRate;
    BigDecimal totalAmount;
    Long createdById;
    String createdByUsername;
    LocalDateTime createdAt;
}
