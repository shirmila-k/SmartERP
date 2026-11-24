package com.factoryflow.erp.controller;

import com.factoryflow.erp.dto.report.*;
import com.factoryflow.erp.service.ExportService;
import com.factoryflow.erp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;

    @GetMapping("/sales")
    public ResponseEntity<SalesRegisterReport> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        return ResponseEntity.ok(reportService.getSalesReport(range[0], range[1]));
    }

    @GetMapping("/gst-summary")
    public ResponseEntity<GstSummaryReport> getGstSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        return ResponseEntity.ok(reportService.getGstSummaryReport(range[0], range[1]));
    }

    @GetMapping("/purchases")
    public ResponseEntity<PurchaseRegisterReport> getPurchaseReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        return ResponseEntity.ok(reportService.getPurchaseReport(range[0], range[1]));
    }

    @GetMapping("/stock-summary")
    public ResponseEntity<StockSummaryReport> getStockSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        return ResponseEntity.ok(reportService.getStockSummaryReport(range[0], range[1]));
    }

    @GetMapping("/stock-ledger")
    public ResponseEntity<AuditStockLedgerReport> getStockLedger(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getAuditStockLedger(itemId, startDate, endDate));
    }

    @GetMapping("/production")
    public ResponseEntity<ProductionReport> getProductionReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        return ResponseEntity.ok(reportService.getProductionReport(range[0], range[1]));
    }

    @GetMapping("/receivables")
    public ResponseEntity<ReceivablesAgeingReport> getReceivablesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        return ResponseEntity.ok(reportService.getReceivablesReport(range[0], range[1]));
    }

    @GetMapping("/{reportType}/export")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String reportType,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        byte[] bytes = exportService.exportReport(reportType, format, range[0], range[1]);

        String contentType = "csv".equalsIgnoreCase(format) ? "text/csv" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String ext = "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
        String filename = String.format("%s-report-%s-to-%s.%s", reportType, range[0], range[1], ext);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    private LocalDate[] resolveDateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            YearMonth ym = YearMonth.now();
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }
        if (start == null) {
            start = end.withDayOfMonth(1);
        }
        if (end == null) {
            end = start.withDayOfMonth(start.lengthOfMonth());
        }
        return new LocalDate[]{start, end};
    }
}
