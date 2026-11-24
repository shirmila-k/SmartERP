package com.factoryflow.erp.controller;

import com.factoryflow.erp.dto.ApproveOrderRequest;
import com.factoryflow.erp.dto.InvoiceResponse;
import com.factoryflow.erp.dto.SalesOrderRequest;
import com.factoryflow.erp.dto.SalesOrderResponse;
import com.factoryflow.erp.service.InvoiceService;
import com.factoryflow.erp.service.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<SalesOrderResponse> createOrder(@Valid @RequestBody SalesOrderRequest request) {
        SalesOrderResponse response = salesOrderService.createSalesOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SalesOrderResponse>> getAllOrders() {
        return ResponseEntity.ok(salesOrderService.getAllOrders());
    }

    @GetMapping("/salesman/{salesmanId}")
    public ResponseEntity<List<SalesOrderResponse>> getOrdersBySalesman(@PathVariable Long salesmanId) {
        return ResponseEntity.ok(salesOrderService.getOrdersBySalesman(salesmanId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<SalesOrderResponse> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(salesOrderService.getOrderById(orderId));
    }

    @PostMapping("/{orderId}/approve-invoice")
    public ResponseEntity<InvoiceResponse> approveAndInvoice(
            @PathVariable Long orderId,
            @RequestBody(required = false) ApproveOrderRequest request) {
        InvoiceResponse response = invoiceService.approveAndGenerateInvoice(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<SalesOrderResponse> rejectOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        SalesOrderResponse response = invoiceService.rejectOrder(orderId, reason);
        return ResponseEntity.ok(response);
    }
}
