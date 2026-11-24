package com.factoryflow.erp.controller;

import com.factoryflow.erp.dto.dashboard.OwnerDashboardResponse;
import com.factoryflow.erp.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping("/owner")
    public ResponseEntity<OwnerDashboardResponse> getOwnerDashboard() {
        return ResponseEntity.ok(ownerDashboardService.getOwnerDashboard());
    }
}
