package com.factoryflow.erp.controller;

import com.factoryflow.erp.dto.CustomerOutstandingResponse;
import com.factoryflow.erp.dto.PaymentRequest;
import com.factoryflow.erp.dto.PaymentResponse;
import com.factoryflow.erp.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/customers/{customerId}/outstanding")
    public ResponseEntity<CustomerOutstandingResponse> getCustomerOutstanding(@PathVariable Long customerId) {
        return ResponseEntity.ok(paymentService.getCustomerOutstanding(customerId));
    }
}
