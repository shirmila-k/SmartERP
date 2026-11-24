package com.factoryflow.erp.controller;

import com.factoryflow.erp.dto.BomResponse;
import com.factoryflow.erp.dto.ProductionRunRequest;
import com.factoryflow.erp.dto.ProductionRunResponse;
import com.factoryflow.erp.service.ProductionRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionRunService productionRunService;

    @PostMapping("/production-runs")
    public ResponseEntity<ProductionRunResponse> createProductionRun(
            @Valid @RequestBody ProductionRunRequest request) {
        ProductionRunResponse response = productionRunService.createProductionRun(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/production-runs")
    public ResponseEntity<List<ProductionRunResponse>> getAllProductionRuns() {
        return ResponseEntity.ok(productionRunService.getAllProductionRuns());
    }

    @GetMapping("/bom/{finishedItemId}")
    public ResponseEntity<BomResponse> getBomByFinishedItemId(@PathVariable Long finishedItemId) {
        return ResponseEntity.ok(productionRunService.getBomByFinishedItemId(finishedItemId));
    }
}
