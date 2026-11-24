package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.ItemType;
import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class InventoryBalanceResponse {
    Long itemId;
    String skuCode;
    String name;
    ItemType itemType;
    UnitOfMeasure unit;
    BigDecimal currentBalance;
    BigDecimal reorderLevel;
    boolean belowReorderLevel;
    BigDecimal standardRate;
    BigDecimal stockValue;
}
