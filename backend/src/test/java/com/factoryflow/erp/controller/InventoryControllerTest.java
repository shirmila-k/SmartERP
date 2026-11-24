package com.factoryflow.erp.controller;

import com.factoryflow.erp.config.SecurityConfig;
import com.factoryflow.erp.domain.enums.ItemType;
import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import com.factoryflow.erp.dto.InventoryBalanceResponse;
import com.factoryflow.erp.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import(SecurityConfig.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void getInventoryBalancesIsPermittedAndReturnsLedgerBalances() throws Exception {
        InventoryBalanceResponse flour = InventoryBalanceResponse.builder()
                .itemId(1L)
                .skuCode("RM-FLOUR")
                .name("Flour")
                .itemType(ItemType.RAW_MATERIAL)
                .unit(UnitOfMeasure.KG)
                .currentBalance(new BigDecimal("100.000"))
                .reorderLevel(new BigDecimal("25.000"))
                .belowReorderLevel(false)
                .standardRate(new BigDecimal("42.00"))
                .stockValue(new BigDecimal("4200.00"))
                .build();

        when(inventoryService.getAllInventoryBalances()).thenReturn(List.of(flour));

        mockMvc.perform(get("/api/inventory/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuCode").value("RM-FLOUR"))
                .andExpect(jsonPath("$[0].currentBalance").value(100.000))
                .andExpect(jsonPath("$[0].standardRate").value(42.00));
    }
}
