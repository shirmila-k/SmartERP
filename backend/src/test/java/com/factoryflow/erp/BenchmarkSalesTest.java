package com.factoryflow.erp;

import com.factoryflow.erp.domain.entity.Customer;
import com.factoryflow.erp.domain.entity.Item;
import com.factoryflow.erp.domain.entity.User;
import com.factoryflow.erp.domain.enums.PaymentMode;
import com.factoryflow.erp.domain.enums.SalesOrderStatus;
import com.factoryflow.erp.dto.*;
import com.factoryflow.erp.repository.CustomerRepository;
import com.factoryflow.erp.repository.ItemRepository;
import com.factoryflow.erp.repository.StockLedgerRepository;
import com.factoryflow.erp.repository.UserRepository;
import com.factoryflow.erp.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class BenchmarkSalesTest {

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
    private PdfInvoiceService pdfInvoiceService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StockLedgerRepository stockLedgerRepository;

    @Test
    @DisplayName("NIT Trichy Benchmark: Production (238 FG) -> Sales Order A (150 TN) -> Invoiced -> Dispatched (88 FG) -> Shortage Order B (120 KA -> 32 Shortage) -> Payment & Receivables")
    void executeSalesAndInvoicingBenchmarkSteps() {
        // Retrieve seeded entities
        User admin = userRepository.findByUsername("admin").orElseThrow();
        User prodSup = userRepository.findByUsername("prod_sup").orElseThrow();
        User salesmanA = userRepository.findByUsername("salesman_a").orElseThrow();
        User salesmanB = userRepository.findByUsername("salesman_b").orElseThrow();

        Customer retailerA = customerRepository.findAll().stream()
                .filter(c -> c.getName().equals("Retailer A")).findFirst().orElseThrow();
        Customer distributorB = customerRepository.findAll().stream()
                .filter(c -> c.getName().equals("Distributor B")).findFirst().orElseThrow();

        Item flour = itemRepository.findBySkuCode("RM-FLOUR").orElseThrow();
        Item oil = itemRepository.findBySkuCode("RM-OIL").orElseThrow();
        Item spice = itemRepository.findBySkuCode("RM-SPICE").orElseThrow();
        Item murukku = itemRepository.findBySkuCode("FG-MURUKKU-200").orElseThrow();

        // 1. Establish initial Phase 2 benchmark production state: 238 packets of Murukku
        purchaseService.createPurchase(PurchaseRequest.builder()
                .supplierName("Sri Krishna Traders")
                .invoiceNo("INV-2026-001")
                .purchaseDate(LocalDate.now())
                .itemId(flour.getId())
                .qty(new BigDecimal("60.000"))
                .unitRate(new BigDecimal("45.00"))
                .createdBy(admin.getId())
                .build());

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

        BigDecimal stockBeforeSales = stockLedgerRepository.findBalanceByItemId(murukku.getId());
        assertThat(stockBeforeSales).isEqualByComparingTo(new BigDecimal("238.000"));

        // STEP 4: Salesman A orders 150 packets for Retailer A (Tamil Nadu)
        SalesOrderResponse orderA = salesOrderService.createSalesOrder(SalesOrderRequest.builder()
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

        assertThat(orderA.getStatus()).isEqualTo(SalesOrderStatus.SUBMITTED);
        assertThat(orderA.isStockShortageFlagged()).isFalse();
        assertThat(orderA.getShortageQty()).isEqualTo(0);

        // Admin approves & invoices order A
        InvoiceResponse invoiceA = invoiceService.approveAndGenerateInvoice(orderA.getId(), null);

        // Intra-State GST Math (Tamil Nadu -> Tamil Nadu):
        // Taxable = 150 * 50.00 = 7,500.00
        // GST Rate = 5%
        // Total Tax = 375.00 -> CGST (2.5%) = 187.50, SGST (2.5%) = 187.50, IGST = 0.00
        // Grand Total = 7,875.00
        assertThat(invoiceA.getTaxableAmount()).isEqualByComparingTo(new BigDecimal("7500.00"));
        assertThat(invoiceA.getCgstAmount()).isEqualByComparingTo(new BigDecimal("187.50"));
        assertThat(invoiceA.getSgstAmount()).isEqualByComparingTo(new BigDecimal("187.50"));
        assertThat(invoiceA.getIgstAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(invoiceA.getGrandTotal()).isEqualByComparingTo(new BigDecimal("7875.00"));

        // STEP 5: Dispatch Invoice A -> stock drops from 238 to 88 packets
        InvoiceResponse dispatchedInvoiceA = invoiceService.dispatchInvoice(invoiceA.getId());
        assertThat(dispatchedInvoiceA.getStatus().name()).isEqualTo("DISPATCHED");

        BigDecimal stockAfterDispatchA = stockLedgerRepository.findBalanceByItemId(murukku.getId());
        assertThat(stockAfterDispatchA).isEqualByComparingTo(new BigDecimal("88.000"));

        // Verify PDF invoice generation
        byte[] pdfBytes = pdfInvoiceService.generateInvoicePdf(invoiceA.getId());
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // STEP 6: Salesman B orders 120 packets for Distributor B (Karnataka)
        // Available stock is now 88 packets -> Shortage = 120 - 88 = 32 packets
        SalesOrderResponse orderB = salesOrderService.createSalesOrder(SalesOrderRequest.builder()
                .customerId(distributorB.getId())
                .salesmanId(salesmanB.getId())
                .lines(List.of(
                        SalesOrderRequest.OrderLineRequest.builder()
                                .itemId(murukku.getId())
                                .qty(120)
                                .unitPrice(new BigDecimal("50.00"))
                                .build()
                ))
                .build());

        assertThat(orderB.getStatus()).isEqualTo(SalesOrderStatus.SUBMITTED);
        assertThat(orderB.isStockShortageFlagged()).isTrue();
        assertThat(orderB.getShortageQty()).isEqualTo(32);

        // Verify IGST quote calculation for Karnataka (Inter-state):
        // 120 packets * 50.00 = 6,000.00 taxable -> IGST (5%) = 300.00
        BigDecimal orderBTaxable = new BigDecimal("120").multiply(new BigDecimal("50.00"));
        BigDecimal orderBIgst = orderBTaxable.multiply(new BigDecimal("0.05"));
        assertThat(orderBIgst).isEqualByComparingTo(new BigDecimal("300.00"));

        // STEP 7: Record 4,000.00 payment from Retailer A -> assert balanceDue == 3,875.00
        PaymentResponse paymentResp = paymentService.recordPayment(PaymentRequest.builder()
                .invoiceId(invoiceA.getId())
                .amountPaid(new BigDecimal("4000.00"))
                .paymentMode(PaymentMode.NEFT)
                .paymentDate(LocalDate.now())
                .recordedBy(admin.getId())
                .build());

        assertThat(paymentResp.getInvoiceGrandTotal()).isEqualByComparingTo(new BigDecimal("7875.00"));
        assertThat(paymentResp.getInvoiceTotalPaid()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(paymentResp.getInvoiceBalanceDue()).isEqualByComparingTo(new BigDecimal("3875.00"));

        // Check Customer Outstanding Receivables API
        CustomerOutstandingResponse outstandingA = paymentService.getCustomerOutstanding(retailerA.getId());
        assertThat(outstandingA.getTotalInvoiced()).isEqualByComparingTo(new BigDecimal("7875.00"));
        assertThat(outstandingA.getTotalReceived()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(outstandingA.getTotalOutstanding()).isEqualByComparingTo(new BigDecimal("3875.00"));
    }
}
