package com.factoryflow.erp;

import com.factoryflow.erp.domain.entity.Item;
import com.factoryflow.erp.domain.entity.User;
import com.factoryflow.erp.dto.ProductionRunRequest;
import com.factoryflow.erp.dto.ProductionRunResponse;
import com.factoryflow.erp.dto.PurchaseRequest;
import com.factoryflow.erp.dto.PurchaseResponse;
import com.factoryflow.erp.repository.ItemRepository;
import com.factoryflow.erp.repository.StockLedgerRepository;
import com.factoryflow.erp.repository.UserRepository;
import com.factoryflow.erp.service.ProductionRunService;
import com.factoryflow.erp.service.PurchaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class BenchmarkProductionTest {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ProductionRunService productionRunService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockLedgerRepository stockLedgerRepository;

    @Test
    @DisplayName("NIT Trichy Benchmark: Purchase Flour -> Produce Murukku 200g -> Verify Ledger, Variances, and FIFO Unit Cost")
    void executeBenchmarkStepsAndVerifyClosingBalancesAndUnitCost() {
        // Retrieve seeded entities
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("admin user not found in benchmark seed"));
        User prodSup = userRepository.findByUsername("prod_sup")
                .orElseThrow(() -> new AssertionError("prod_sup user not found in benchmark seed"));

        Item flour = itemRepository.findBySkuCode("RM-FLOUR")
                .orElseThrow(() -> new AssertionError("RM-FLOUR not found"));
        Item oil = itemRepository.findBySkuCode("RM-OIL")
                .orElseThrow(() -> new AssertionError("RM-OIL not found"));
        Item spice = itemRepository.findBySkuCode("RM-SPICE")
                .orElseThrow(() -> new AssertionError("RM-SPICE not found"));
        Item pouch = itemRepository.findBySkuCode("PK-POUCH-200")
                .orElseThrow(() -> new AssertionError("PK-POUCH-200 not found"));
        Item carton = itemRepository.findBySkuCode("PK-CARTON-20")
                .orElseThrow(() -> new AssertionError("PK-CARTON-20 not found"));
        Item murukku = itemRepository.findBySkuCode("FG-MURUKKU-200")
                .orElseThrow(() -> new AssertionError("FG-MURUKKU-200 not found"));

        // STEP 1: Purchase 60 kg flour @ ₹45/kg
        PurchaseRequest purchaseRequest = PurchaseRequest.builder()
                .supplierName("Sri Krishna Traders")
                .invoiceNo("INV-2026-001")
                .purchaseDate(LocalDate.now())
                .itemId(flour.getId())
                .qty(new BigDecimal("60.000"))
                .unitRate(new BigDecimal("45.00"))
                .createdBy(admin.getId())
                .build();

        PurchaseResponse purchaseResponse = purchaseService.createPurchase(purchaseRequest);
        assertThat(purchaseResponse.getTotalAmount()).isEqualByComparingTo(new BigDecimal("2700.00"));

        // Flour balance after purchase should be 100 + 60 = 160 kg
        BigDecimal flourBalanceAfterPurchase = stockLedgerRepository.findBalanceByItemId(flour.getId());
        assertThat(flourBalanceAfterPurchase).isEqualByComparingTo(new BigDecimal("160.000"));

        // STEP 2 & 3: Run 1 batch of Murukku 200 g
        // 40 kg flour, 10.5 L oil, 1.2 kg spice mix, 188 good packets, 6 damaged pouches, 9 cartons, ₹1200 labour, ₹600 overhead
        ProductionRunRequest runRequest = ProductionRunRequest.builder()
                .runCode("RUN-MURUKKU-001")
                .finishedItemId(murukku.getId())
                .batchesPlanned(new BigDecimal("1.00"))
                .rawConsumptions(List.of(
                        ProductionRunRequest.RawConsumptionItem.builder()
                                .itemId(flour.getId())
                                .actualQty(new BigDecimal("40.000"))
                                .build(),
                        ProductionRunRequest.RawConsumptionItem.builder()
                                .itemId(oil.getId())
                                .actualQty(new BigDecimal("10.500"))
                                .build(),
                        ProductionRunRequest.RawConsumptionItem.builder()
                                .itemId(spice.getId())
                                .actualQty(new BigDecimal("1.200"))
                                .build()
                ))
                .actualGoodPackets(188)
                .damagedPouches(6)
                .cartonsUsed(9)
                .labourCost(new BigDecimal("1200.00"))
                .overheadCost(new BigDecimal("600.00"))
                .runDate(LocalDate.now())
                .createdBy(prodSup.getId())
                .build();

        ProductionRunResponse runResponse = productionRunService.createProductionRun(runRequest);

        // STEP 8: Assert exact closing ledger balances
        // Flour = 100 (seed) + 60 (purchase) - 40 (issued) = 120.000
        BigDecimal flourClosing = stockLedgerRepository.findBalanceByItemId(flour.getId());
        assertThat(flourClosing).isEqualByComparingTo(new BigDecimal("120.000"));

        // Oil = 30 (seed) - 10.5 (issued) = 19.500
        BigDecimal oilClosing = stockLedgerRepository.findBalanceByItemId(oil.getId());
        assertThat(oilClosing).isEqualByComparingTo(new BigDecimal("19.500"));

        // Spice mix = 5 (seed) - 1.2 (issued) = 3.800
        BigDecimal spiceClosing = stockLedgerRepository.findBalanceByItemId(spice.getId());
        assertThat(spiceClosing).isEqualByComparingTo(new BigDecimal("3.800"));

        // Pouches = 500 (seed) - 188 (good) - 6 (damaged) = 306.000
        BigDecimal pouchClosing = stockLedgerRepository.findBalanceByItemId(pouch.getId());
        assertThat(pouchClosing).isEqualByComparingTo(new BigDecimal("306.000"));

        // Cartons = 30 (seed) - 9 (issued) = 21.000
        BigDecimal cartonClosing = stockLedgerRepository.findBalanceByItemId(carton.getId());
        assertThat(cartonClosing).isEqualByComparingTo(new BigDecimal("21.000"));

        // Murukku 200 g = 50 (seed) + 188 (produced) = 238.000
        BigDecimal murukkuClosing = stockLedgerRepository.findBalanceByItemId(murukku.getId());
        assertThat(murukkuClosing).isEqualByComparingTo(new BigDecimal("238.000"));

        // Assert yield variance: (188 - 200) / 200 * 100 = -6.00% (yieldFlagged == true)
        assertThat(runResponse.getYieldVariancePct()).isEqualByComparingTo(new BigDecimal("-6.00"));
        assertThat(runResponse.isYieldFlagged()).isTrue();

        // Assert Oil variance: (10.5 - 10.0) / 10.0 * 100 = +5.00% (varianceFlagged == true)
        ProductionRunResponse.ConsumptionLineDto oilConsumption = runResponse.getConsumptions().stream()
                .filter(c -> c.getItemId().equals(oil.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Oil consumption not found"));
        assertThat(oilConsumption.getVariancePct()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(oilConsumption.isVarianceFlagged()).isTrue();

        // Assert Flour variance: 40.0 vs 40.0 = 0.00% (varianceFlagged == false)
        ProductionRunResponse.ConsumptionLineDto flourConsumption = runResponse.getConsumptions().stream()
                .filter(c -> c.getItemId().equals(flour.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Flour consumption not found"));
        assertThat(flourConsumption.getVariancePct()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(flourConsumption.isVarianceFlagged()).isFalse();

        // Assert FIFO Costing Math:
        // Flour: 40 kg from opening @ 42 = 1,680.00
        // Oil: 10.5 L from opening @ 140 = 1,470.00
        // Spice: 1.2 kg from opening @ 200 = 240.00
        // Total Raw: 3,390.00
        // Pouches: (188 + 6) = 194 pouches from opening @ 2.50 = 485.00
        // Cartons: 9 cartons from opening @ 15.00 = 135.00
        // Total Packaging: 620.00
        // Labour: 1,200.00
        // Overhead: 600.00
        // Total Batch Cost = 3,390 + 620 + 1,200 + 600 = 5,810.00
        // Calculated Unit Cost = 5,810.00 / 188 = 30.904255... -> 30.90
        assertThat(runResponse.getTotalBatchCost()).isEqualByComparingTo(new BigDecimal("5810.00"));
        assertThat(runResponse.getCalculatedUnitCost()).isEqualByComparingTo(new BigDecimal("30.90"));

        // Verify updated finished item standard rate
        Item updatedMurukku = itemRepository.findById(murukku.getId()).orElseThrow();
        assertThat(updatedMurukku.getStandardRate()).isEqualByComparingTo(new BigDecimal("30.90"));
    }
}
