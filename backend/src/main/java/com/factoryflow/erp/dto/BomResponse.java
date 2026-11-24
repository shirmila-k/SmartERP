package com.factoryflow.erp.dto;

import com.factoryflow.erp.domain.enums.ItemType;
import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class BomResponse {
    Long bomId;
    Long finishedItemId;
    String finishedItemSku;
    String finishedItemName;
    String recipeGroup;
    Integer expectedYieldPackets;
    List<BomLineDto> lines;

    @Value
    @Builder
    public static class BomLineDto {
        Long id;
        Long componentItemId;
        String componentSkuCode;
        String componentName;
        ItemType componentType;
        UnitOfMeasure unit;
        BigDecimal standardQty;
        boolean isPackaging;
    }
}
