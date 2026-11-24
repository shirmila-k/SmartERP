package com.factoryflow.erp.controller;

import com.factoryflow.erp.dto.InventoryBalanceResponse;
import com.factoryflow.erp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/balances")
    public ResponseEntity<List<InventoryBalanceResponse>> getInventoryBalances() {
        return ResponseEntity.ok(inventoryService.getAllInventoryBalances());
    }
}
