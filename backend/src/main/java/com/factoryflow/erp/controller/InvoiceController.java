package com.factoryflow.erp.controller;

import com.factoryflow.erp.dto.InvoiceResponse;
import com.factoryflow.erp.service.InvoiceService;
import com.factoryflow.erp.service.PdfInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfInvoiceService pdfInvoiceService;

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    @PostMapping("/{invoiceId}/dispatch")
    public ResponseEntity<InvoiceResponse> dispatchInvoice(@PathVariable Long invoiceId) {
        InvoiceResponse response = invoiceService.dispatchInvoice(invoiceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable Long invoiceId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = body != null && body.get("userId") != null
                ? Long.valueOf(body.get("userId").toString())
                : null;
        String reason = body != null && body.get("reason") != null
                ? body.get("reason").toString()
                : "Customer cancellation";

        InvoiceResponse response = invoiceService.cancelInvoice(invoiceId, userId, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{invoiceId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long invoiceId) {
        byte[] pdfBytes = pdfInvoiceService.generateInvoicePdf(invoiceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + invoiceId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
