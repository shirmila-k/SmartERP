package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.SalesOrderStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class SalesOrderResponse {
    Long id;
    String orderNumber;
    Long customerId;
    String customerName;
    String customerState;
    String customerGstin;
    Long salesmanId;
    String salesmanName;
    SalesOrderStatus status;
    boolean stockShortageFlagged;
    Integer shortageQty;
    LocalDateTime orderDate;
    BigDecimal totalEstimatedAmount;
    List<SalesOrderLineDto> lines;

    @Value
    @Builder
    public static class SalesOrderLineDto {
        Long id;
        Long itemId;
        String skuCode;
        String itemName;
        Integer qty;
        BigDecimal unitPrice;
        BigDecimal lineTotal;
    }
}
