package com.factoryflow.erp;

import com.factoryflow.erp.domain.entity.Customer;
import com.factoryflow.erp.domain.entity.Item;
import com.factoryflow.erp.domain.entity.User;
import com.factoryflow.erp.domain.enums.PaymentMode;
import com.factoryflow.erp.dto.PaymentRequest;
import com.factoryflow.erp.dto.ProductionRunRequest;
import com.factoryflow.erp.dto.PurchaseRequest;
import com.factoryflow.erp.dto.SalesOrderRequest;
import com.factoryflow.erp.dto.report.GstSummaryReport;
import com.factoryflow.erp.dto.report.ReceivablesAgeingReport;
import com.factoryflow.erp.dto.report.StockSummaryReport;
import com.factoryflow.erp.repository.CustomerRepository;
import com.factoryflow.erp.repository.ItemRepository;
import com.factoryflow.erp.repository.UserRepository;
import com.factoryflow.erp.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class BenchmarkReportsTest {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ProductionRunService productionRunService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("NIT Trichy Benchmark: Reports Verification (Stock Summary, GST Outward, Receivables Ageing, and XLSX Export)")
    void verifyBenchmarkReportsAndExcelExport() throws IOException {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        User prodSup = userRepository.findByUsername("prod_sup").orElseThrow();
        User salesmanA = userRepository.findByUsername("salesman_a").orElseThrow();

        Customer retailerA = customerRepository.findAll().stream()
                .filter(c -> c.getName().equals("Retailer A")).findFirst().orElseThrow();

        Item flour = itemRepository.findBySkuCode("RM-FLOUR").orElseThrow();
        Item oil = itemRepository.findBySkuCode("RM-OIL").orElseThrow();
        Item spice = itemRepository.findBySkuCode("RM-SPICE").orElseThrow();
        Item pouch = itemRepository.findBySkuCode("PK-POUCH-200").orElseThrow();
        Item carton = itemRepository.findBySkuCode("PK-CARTON-20").orElseThrow();
        Item murukku = itemRepository.findBySkuCode("FG-MURUKKU-200").orElseThrow();

        // 1. Purchase: 60 kg Flour @ 45
        purchaseService.createPurchase(PurchaseRequest.builder()
                .supplierName("Sri Krishna Traders")
                .invoiceNo("INV-2026-001")
                .purchaseDate(LocalDate.now())
                .itemId(flour.getId())
                .qty(new BigDecimal("60.000"))
                .unitRate(new BigDecimal("45.00"))
                .createdBy(admin.getId())
                .build());

        // 2. Production: 1 batch of Murukku 200 g
        productionRunService.createProductionRun(ProductionRunRequest.builder()
                .runCode("RUN-MURUKKU-001")
                .finishedItemId(murukku.getId())
                .batchesPlanned(new BigDecimal("1.00"))
                .rawConsumptions(List.of(
                        ProductionRunRequest.RawConsumptionItem.builder().itemId(flour.getId()).actualQty(new BigDecimal("40.000")).build(),
                        ProductionRunRequest.RawConsumptionItem.builder().itemId(oil.getId()).actualQty(new BigDecimal("10.500")).build(),
                        ProductionRunRequest.RawConsumptionItem.builder().itemId(spice.getId()).actualQty(new BigDecimal("1.200")).build()
                ))
                .actualGoodPackets(188)
                .damagedPouches(6)
                .cartonsUsed(9)
                .labourCost(new BigDecimal("1200.00"))
                .overheadCost(new BigDecimal("600.00"))
                .runDate(LocalDate.now())
                .createdBy(prodSup.getId())
                .build());

        // 3. Sales Order & Invoice: 150 packets for Retailer A (Tamil Nadu)
        var orderA = salesOrderService.createSalesOrder(SalesOrderRequest.builder()
                .customerId(retailerA.getId())
                .salesmanId(salesmanA.getId())
                .lines(List.of(
                        SalesOrderRequest.OrderLineRequest.builder()
                                .itemId(murukku.getId())
                                .qty(150)
                                .unitPrice(new BigDecimal("50.00"))
                                .build()
                ))
                .build());

        var invoiceA = invoiceService.approveAndGenerateInvoice(orderA.getId(), null);

        // 4. Dispatch: Drops Murukku to 88 packets
        invoiceService.dispatchInvoice(invoiceA.getId());

        // 5. Payment: ₹4,000 against Invoice A
        paymentService.recordPayment(PaymentRequest.builder()
                .invoiceId(invoiceA.getId())
                .amountPaid(new BigDecimal("4000.00"))
                .paymentMode(PaymentMode.NEFT)
                .paymentDate(LocalDate.now())
                .recordedBy(admin.getId())
                .build());

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // --- VERIFY STOCK SUMMARY ---
        StockSummaryReport stockSummary = reportService.getStockSummaryReport(startOfMonth, endOfMonth);
        assertThat(stockSummary.getItems()).isNotEmpty();

        StockSummaryReport.StockSummaryItem flourItem = stockSummary.getItems().stream()
                .filter(i -> i.getSkuCode().equals("RM-FLOUR")).findFirst().orElseThrow();
        assertThat(flourItem.getClosingBalance()).isEqualByComparingTo(new BigDecimal("120.000"));

        StockSummaryReport.StockSummaryItem oilItem = stockSummary.getItems().stream()
                .filter(i -> i.getSkuCode().equals("RM-OIL")).findFirst().orElseThrow();
        assertThat(oilItem.getClosingBalance()).isEqualByComparingTo(new BigDecimal("19.500"));

        StockSummaryReport.StockSummaryItem pouchItem = stockSummary.getItems().stream()
                .filter(i -> i.getSkuCode().equals("PK-POUCH-200")).findFirst().orElseThrow();
        assertThat(pouchItem.getClosingBalance()).isEqualByComparingTo(new BigDecimal("306.000"));

        StockSummaryReport.StockSummaryItem murukkuItem = stockSummary.getItems().stream()
                .filter(i -> i.getSkuCode().equals("FG-MURUKKU-200")).findFirst().orElseThrow();
        assertThat(murukkuItem.getClosingBalance()).isEqualByComparingTo(new BigDecimal("88.000"));

        // --- VERIFY GST SUMMARY ---
        GstSummaryReport gstSummary = reportService.getGstSummaryReport(startOfMonth, endOfMonth);
        assertThat(gstSummary.getTotalTaxable()).isEqualByComparingTo(new BigDecimal("7500.00"));
        assertThat(gstSummary.getTotalCgst()).isEqualByComparingTo(new BigDecimal("187.50"));
        assertThat(gstSummary.getTotalSgst()).isEqualByComparingTo(new BigDecimal("187.50"));
        assertThat(gstSummary.getTotalIgst()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(gstSummary.getTotalTax()).isEqualByComparingTo(new BigDecimal("375.00"));

        // --- VERIFY RECEIVABLES AGEING ---
        ReceivablesAgeingReport receivables = reportService.getReceivablesReport(startOfMonth, endOfMonth);
        ReceivablesAgeingReport.CustomerAgeing retailerAgeing = receivables.getCustomerAgeing().stream()
                .filter(c -> c.getCustomerName().equals("Retailer A")).findFirst().orElseThrow();
        assertThat(retailerAgeing.getTotalInvoiced()).isEqualByComparingTo(new BigDecimal("7875.00"));
        assertThat(retailerAgeing.getTotalPaid()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(retailerAgeing.getBalanceDue()).isEqualByComparingTo(new BigDecimal("3875.00"));

        // --- VERIFY EXCEL EXPORT (.xlsx) ---
        byte[] xlsxBytes = exportService.exportReport("stock-summary", "xlsx", startOfMonth, endOfMonth);
        assertThat(xlsxBytes).isNotEmpty();
        assertThat(xlsxBytes.length).isGreaterThan(1000);

        // Verify CSV export (.csv)
        byte[] csvBytes = exportService.exportReport("stock-summary", "csv", startOfMonth, endOfMonth);
        assertThat(csvBytes).isNotEmpty();
        String csvContent = new String(csvBytes);
        assertThat(csvContent).contains("RM-FLOUR").contains("120.000");
    }
}
