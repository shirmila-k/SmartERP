package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.*;
import com.factoryflow.erp.domain.enums.InvoiceStatus;
import com.factoryflow.erp.domain.enums.ItemType;
import com.factoryflow.erp.domain.enums.MovementType;
import com.factoryflow.erp.dto.report.*;
import com.factoryflow.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductionRunRepository productionRunRepository;
    private final ItemRepository itemRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    private static final String FACTORY_STATE = "Tamil Nadu";

    /**
     * 1. Sales Register & Customer/Product Breakdown
     */
    @Transactional(readOnly = true)
    public SalesRegisterReport getSalesReport(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .filter(inv -> !inv.getInvoiceDate().isBefore(startDate) && !inv.getInvoiceDate().isAfter(endDate))
                .sorted(Comparator.comparing(Invoice::getInvoiceDate).thenComparing(Invoice::getId))
                .toList();

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal totalGrand = BigDecimal.ZERO;

        List<SalesRegisterReport.SalesRegisterEntry> entries = new ArrayList<>();
        Map<Long, List<Invoice>> byCustomer = new HashMap<>();
        Map<String, int[]> productQtyMap = new HashMap<>(); // sku -> [qty]
        Map<String, BigDecimal> productTaxableMap = new HashMap<>(); // sku -> taxable
        Map<String, String> productNameMap = new HashMap<>();

        for (Invoice inv : invoices) {
            totalTaxable = totalTaxable.add(inv.getTaxableAmount());
            totalCgst = totalCgst.add(inv.getCgstAmount());
            totalSgst = totalSgst.add(inv.getSgstAmount());
            totalIgst = totalIgst.add(inv.getIgstAmount());
            totalGrand = totalGrand.add(inv.getGrandTotal());

            entries.add(SalesRegisterReport.SalesRegisterEntry.builder()
                    .invoiceId(inv.getId())
                    .invoiceNumber(inv.getInvoiceNumber())
                    .invoiceDate(inv.getInvoiceDate())
                    .customerName(inv.getCustomer().getName())
                    .customerGstin(inv.getCustomer().getGstin())
                    .stateName(inv.getCustomer().getStateName())
                    .taxableAmount(inv.getTaxableAmount())
                    .cgstAmount(inv.getCgstAmount())
                    .sgstAmount(inv.getSgstAmount())
                    .igstAmount(inv.getIgstAmount())
                    .grandTotal(inv.getGrandTotal())
                    .status(inv.getStatus().name())
                    .build());

            byCustomer.computeIfAbsent(inv.getCustomer().getId(), k -> new ArrayList<>()).add(inv);

            if (inv.getOrder() != null && inv.getOrder().getLines() != null) {
                for (SalesOrderLine line : inv.getOrder().getLines()) {
                    String sku = line.getItem().getSkuCode();
                    productNameMap.put(sku, line.getItem().getName());
                    productQtyMap.computeIfAbsent(sku, k -> new int[]{0})[0] += line.getQty();

                    BigDecimal lineTaxable = BigDecimal.valueOf(line.getQty()).multiply(line.getUnitPrice());
                    productTaxableMap.merge(sku, lineTaxable, BigDecimal::add);
                }
            }
        }

        // Customer breakdown
        List<SalesRegisterReport.CustomerBreakdown> customerBreakdown = byCustomer.values().stream().map(cInvoices -> {
            Invoice first = cInvoices.get(0);
            BigDecimal cTaxable = cInvoices.stream().map(Invoice::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cTax = cInvoices.stream().map(i -> i.getCgstAmount().add(i.getSgstAmount()).add(i.getIgstAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cGrand = cInvoices.stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

            return SalesRegisterReport.CustomerBreakdown.builder()
                    .customerName(first.getCustomer().getName())
                    .gstin(first.getCustomer().getGstin())
                    .stateName(first.getCustomer().getStateName())
                    .totalInvoices(cInvoices.size())
                    .totalTaxable(cTaxable.setScale(2, RoundingMode.HALF_UP))
                    .totalTax(cTax.setScale(2, RoundingMode.HALF_UP))
                    .totalAmount(cGrand.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }).toList();

        // Product breakdown
        List<SalesRegisterReport.ProductBreakdown> productBreakdown = productQtyMap.entrySet().stream().map(entry -> {
            String sku = entry.getKey();
            return SalesRegisterReport.ProductBreakdown.builder()
                    .skuCode(sku)
                    .productName(productNameMap.get(sku))
                    .totalQuantity(entry.getValue()[0])
                    .totalTaxable(productTaxableMap.getOrDefault(sku, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                    .build();
        }).toList();

        return SalesRegisterReport.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalTaxableAmount(totalTaxable.setScale(2, RoundingMode.HALF_UP))
                .totalCgstAmount(totalCgst.setScale(2, RoundingMode.HALF_UP))
                .totalSgstAmount(totalSgst.setScale(2, RoundingMode.HALF_UP))
                .totalIgstAmount(totalIgst.setScale(2, RoundingMode.HALF_UP))
                .totalGrandAmount(totalGrand.setScale(2, RoundingMode.HALF_UP))
                .entries(entries)
                .customerBreakdown(customerBreakdown)
                .productBreakdown(productBreakdown)
                .build();
    }

    /**
     * 2. GST Outward Supplies Summary (GSTR-1 / GSTR-3B)
     */
    @Transactional(readOnly = true)
    public GstSummaryReport getGstSummaryReport(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .filter(inv -> !inv.getInvoiceDate().isBefore(startDate) && !inv.getInvoiceDate().isAfter(endDate))
                .toList();

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        List<Invoice> b2bInvoices = new ArrayList<>();
        List<Invoice> b2cInvoices = new ArrayList<>();
        Map<String, List<Invoice>> byState = new HashMap<>();

        Map<String, Integer> hsnQty = new HashMap<>();
        Map<String, BigDecimal> hsnTaxable = new HashMap<>();
        Map<String, BigDecimal> hsnCgst = new HashMap<>();
        Map<String, BigDecimal> hsnSgst = new HashMap<>();
        Map<String, BigDecimal> hsnIgst = new HashMap<>();

        for (Invoice inv : invoices) {
            totalTaxable = totalTaxable.add(inv.getTaxableAmount());
            totalCgst = totalCgst.add(inv.getCgstAmount());
            totalSgst = totalSgst.add(inv.getSgstAmount());
            totalIgst = totalIgst.add(inv.getIgstAmount());

            String gstin = inv.getCustomer().getGstin();
            if (gstin != null && !gstin.trim().isEmpty() && !gstin.equalsIgnoreCase("URP")) {
                b2bInvoices.add(inv);
            } else {
                b2cInvoices.add(inv);
            }

            byState.computeIfAbsent(inv.getCustomer().getStateName(), k -> new ArrayList<>()).add(inv);

            boolean isIntra = inv.getCustomer().getStateName() != null &&
                    inv.getCustomer().getStateName().trim().equalsIgnoreCase(FACTORY_STATE);

            if (inv.getOrder() != null && inv.getOrder().getLines() != null) {
                for (SalesOrderLine line : inv.getOrder().getLines()) {
                    String hsn = line.getItem().getHsnCode() != null ? line.getItem().getHsnCode() : "21069099";
                    hsnQty.merge(hsn, line.getQty(), Integer::sum);

                    BigDecimal lineTaxable = BigDecimal.valueOf(line.getQty()).multiply(line.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
                    hsnTaxable.merge(hsn, lineTaxable, BigDecimal::add);

                    BigDecimal rate = line.getItem().getGstRate() != null ? line.getItem().getGstRate() : BigDecimal.ZERO;
                    BigDecimal tax = lineTaxable.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                    if (isIntra) {
                        BigDecimal half = tax.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                        hsnCgst.merge(hsn, half, BigDecimal::add);
                        hsnSgst.merge(hsn, half, BigDecimal::add);
                    } else {
                        hsnIgst.merge(hsn, tax, BigDecimal::add);
                    }
                }
            }
        }

        // B2B vs B2C summary
        List<GstSummaryReport.B2bVsB2cSummary> b2bVsB2cList = List.of(
                summarizeB2B("B2B", b2bInvoices),
                summarizeB2B("B2C", b2cInvoices)
        );

        // Place of Supply summary
        List<GstSummaryReport.PlaceOfSupplySummary> posList = byState.entrySet().stream().map(entry -> {
            String state = entry.getKey();
            List<Invoice> stateInvoices = entry.getValue();
            boolean isIntra = state != null && state.trim().equalsIgnoreCase(FACTORY_STATE);

            BigDecimal sTaxable = stateInvoices.stream().map(Invoice::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sCgst = stateInvoices.stream().map(Invoice::getCgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sSgst = stateInvoices.stream().map(Invoice::getSgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sIgst = stateInvoices.stream().map(Invoice::getIgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sTax = sCgst.add(sSgst).add(sIgst);

            return GstSummaryReport.PlaceOfSupplySummary.builder()
                    .stateName(state)
                    .supplyType(isIntra ? "INTRA_STATE" : "INTER_STATE")
                    .invoiceCount(stateInvoices.size())
                    .taxableAmount(sTaxable.setScale(2, RoundingMode.HALF_UP))
                    .cgstAmount(sCgst.setScale(2, RoundingMode.HALF_UP))
                    .sgstAmount(sSgst.setScale(2, RoundingMode.HALF_UP))
                    .igstAmount(sIgst.setScale(2, RoundingMode.HALF_UP))
                    .totalTax(sTax.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }).toList();

        // HSN Summary
        List<GstSummaryReport.HsnSummary> hsnList = hsnQty.keySet().stream().map(hsn -> {
            BigDecimal tVal = hsnTaxable.getOrDefault(hsn, BigDecimal.ZERO);
            BigDecimal cg = hsnCgst.getOrDefault(hsn, BigDecimal.ZERO);
            BigDecimal sg = hsnSgst.getOrDefault(hsn, BigDecimal.ZERO);
            BigDecimal ig = hsnIgst.getOrDefault(hsn, BigDecimal.ZERO);
            return GstSummaryReport.HsnSummary.builder()
                    .hsnCode(hsn)
                    .description("Snacks / Murukku")
                    .totalQuantity(hsnQty.getOrDefault(hsn, 0))
                    .taxableValue(tVal.setScale(2, RoundingMode.HALF_UP))
                    .cgstAmount(cg.setScale(2, RoundingMode.HALF_UP))
                    .sgstAmount(sg.setScale(2, RoundingMode.HALF_UP))
                    .igstAmount(ig.setScale(2, RoundingMode.HALF_UP))
                    .totalTax(cg.add(sg).add(ig).setScale(2, RoundingMode.HALF_UP))
                    .build();
        }).toList();

        BigDecimal totalTax = totalCgst.add(totalSgst).add(totalIgst).setScale(2, RoundingMode.HALF_UP);

        return GstSummaryReport.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalTaxable(totalTaxable.setScale(2, RoundingMode.HALF_UP))
                .totalCgst(totalCgst.setScale(2, RoundingMode.HALF_UP))
                .totalSgst(totalSgst.setScale(2, RoundingMode.HALF_UP))
                .totalIgst(totalIgst.setScale(2, RoundingMode.HALF_UP))
                .totalTax(totalTax)
                .b2bVsB2c(b2bVsB2cList)
                .placeOfSupplySummary(posList)
                .hsnSummary(hsnList)
                .build();
    }

    private GstSummaryReport.B2bVsB2cSummary summarizeB2B(String category, List<Invoice> invs) {
        BigDecimal taxable = invs.stream().map(Invoice::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgst = invs.stream().map(Invoice::getCgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sgst = invs.stream().map(Invoice::getSgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igst = invs.stream().map(Invoice::getIgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grand = invs.stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        return GstSummaryReport.B2bVsB2cSummary.builder()
                .category(category)
                .invoiceCount(invs.size())
                .taxableAmount(taxable.setScale(2, RoundingMode.HALF_UP))
                .cgstAmount(cgst.setScale(2, RoundingMode.HALF_UP))
                .sgstAmount(sgst.setScale(2, RoundingMode.HALF_UP))
                .igstAmount(igst.setScale(2, RoundingMode.HALF_UP))
                .totalTax(cgst.add(sgst).add(igst).setScale(2, RoundingMode.HALF_UP))
                .grandTotal(grand.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * 3. Purchase Register
     */
    @Transactional(readOnly = true)
    public PurchaseRegisterReport getPurchaseReport(LocalDate startDate, LocalDate endDate) {
        List<Purchase> purchases = purchaseRepository.findAll(Sort.by(Sort.Direction.ASC, "purchaseDate")).stream()
                .filter(p -> !p.getPurchaseDate().isBefore(startDate) && !p.getPurchaseDate().isAfter(endDate))
                .toList();

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<PurchaseRegisterReport.PurchaseEntry> entries = new ArrayList<>();

        for (Purchase p : purchases) {
            grandTotal = grandTotal.add(p.getTotalAmount());
            entries.add(PurchaseRegisterReport.PurchaseEntry.builder()
                    .purchaseId(p.getId())
                    .purchaseDate(p.getPurchaseDate())
                    .supplierName(p.getSupplierName())
                    .invoiceNo(p.getInvoiceNo())
                    .itemName(p.getItem().getName())
                    .skuCode(p.getItem().getSkuCode())
                    .unit(p.getItem().getUnit())
                    .qty(p.getQty())
                    .unitRate(p.getUnitRate())
                    .totalAmount(p.getTotalAmount())
                    .build());
        }

        return PurchaseRegisterReport.builder()
                .startDate(startDate)
                .endDate(endDate)
                .grandTotalSpend(grandTotal.setScale(2, RoundingMode.HALF_UP))
                .entries(entries)
                .build();
    }

    /**
     * 4. Stock Summary & Audit Stock Ledger
     */
    @Transactional(readOnly = true)
    public StockSummaryReport getStockSummaryReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.atTime(LocalTime.MAX);

        List<Item> items = itemRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<StockLedger> allLedger = stockLedgerRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));

        List<StockSummaryReport.StockSummaryItem> summaryItems = new ArrayList<>();

        for (Item item : items) {
            List<StockLedger> itemEntries = allLedger.stream()
                    .filter(l -> l.getItem().getId().equals(item.getId()))
                    .toList();

            BigDecimal opening = BigDecimal.ZERO;
            BigDecimal receipts = BigDecimal.ZERO;
            BigDecimal issues = BigDecimal.ZERO;

            for (StockLedger entry : itemEntries) {
                LocalDateTime entryTime = entry.getCreatedAt();
                BigDecimal change = entry.getQtyChange();

                // Opening: strictly before startDt OR (OPENING on startDate)
                if (entryTime.isBefore(startDt) || (entry.getMovementType() == MovementType.OPENING && !entryTime.isAfter(endDt))) {
                    opening = opening.add(change);
                } else if (!entryTime.isBefore(startDt) && !entryTime.isAfter(endDt)) {
                    if (change.compareTo(BigDecimal.ZERO) > 0) {
                        receipts = receipts.add(change);
                    } else {
                        issues = issues.add(change); // negative qty
                    }
                }
            }

            BigDecimal closing = opening.add(receipts).add(issues).setScale(3, RoundingMode.HALF_UP);
            BigDecimal rate = item.getStandardRate() != null ? item.getStandardRate() : BigDecimal.ZERO;
            BigDecimal val = closing.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            summaryItems.add(StockSummaryReport.StockSummaryItem.builder()
                    .itemId(item.getId())
                    .skuCode(item.getSkuCode())
                    .itemName(item.getName())
                    .itemType(item.getItemType())
                    .unit(item.getUnit())
                    .openingBalance(opening.setScale(3, RoundingMode.HALF_UP))
                    .receipts(receipts.setScale(3, RoundingMode.HALF_UP))
                    .issuesAndSales(issues.setScale(3, RoundingMode.HALF_UP))
                    .closingBalance(closing)
                    .standardRate(rate.setScale(2, RoundingMode.HALF_UP))
                    .closingStockValue(val)
                    .build());
        }

        return StockSummaryReport.builder()
                .startDate(startDate)
                .endDate(endDate)
                .items(summaryItems)
                .build();
    }

    @Transactional(readOnly = true)
    public AuditStockLedgerReport getAuditStockLedger(Long itemId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : LocalDate.of(2000, 1, 1).atStartOfDay();
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.of(2100, 1, 1).atTime(LocalTime.MAX);

        List<StockLedger> entries = stockLedgerRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .filter(l -> itemId == null || l.getItem().getId().equals(itemId))
                .filter(l -> !l.getCreatedAt().isBefore(startDt) && !l.getCreatedAt().isAfter(endDt))
                .toList();

        List<AuditStockLedgerReport.StockLedgerRow> rows = entries.stream().map(l -> AuditStockLedgerReport.StockLedgerRow.builder()
                .id(l.getId())
                .createdAt(l.getCreatedAt())
                .itemSku(l.getItem().getSkuCode())
                .itemName(l.getItem().getName())
                .movementType(l.getMovementType())
                .qtyChange(l.getQtyChange())
                .unitRate(l.getUnitRate())
                .sourceType(l.getSourceType())
                .sourceId(l.getSourceId())
                .remarks(l.getRemarks())
                .build()).toList();

        return AuditStockLedgerReport.builder()
                .filteredItemId(itemId)
                .rows(rows)
                .build();
    }

    /**
     * 5. Production Variance & Costing Report
     */
    @Transactional(readOnly = true)
    public ProductionReport getProductionReport(LocalDate startDate, LocalDate endDate) {
        List<ProductionRun> runs = productionRunRepository.findAll(Sort.by(Sort.Direction.ASC, "runDate")).stream()
                .filter(r -> !r.getRunDate().isBefore(startDate) && !r.getRunDate().isAfter(endDate))
                .toList();

        int totalOutput = 0;
        BigDecimal totalLabour = BigDecimal.ZERO;
        BigDecimal totalOverhead = BigDecimal.ZERO;
        List<ProductionReport.ProductionRunDetail> details = new ArrayList<>();

        for (ProductionRun r : runs) {
            totalOutput += r.getActualGoodPackets();
            totalLabour = totalLabour.add(r.getLabourCost());
            totalOverhead = totalOverhead.add(r.getOverheadCost());

            List<ProductionReport.ConsumptionVariance> cvList = r.getConsumptions().stream()
                    .map(c -> ProductionReport.ConsumptionVariance.builder()
                            .componentSku(c.getItem().getSkuCode())
                            .componentName(c.getItem().getName())
                            .plannedQty(c.getPlannedQty())
                            .actualQty(c.getActualQty())
                            .variancePct(c.getVariancePct())
                            .varianceFlagged(Boolean.TRUE.equals(c.getVarianceFlagged()))
                            .unitCostApplied(c.getUnitCostApplied())
                            .build())
                    .toList();

            details.add(ProductionReport.ProductionRunDetail.builder()
                    .runId(r.getId())
                    .runCode(r.getRunCode())
                    .runDate(r.getRunDate())
                    .productName(r.getFinishedItem().getName())
                    .productSku(r.getFinishedItem().getSkuCode())
                    .batchesPlanned(r.getBatchesPlanned())
                    .expectedOutputPackets(r.getExpectedOutputPackets())
                    .actualGoodPackets(r.getActualGoodPackets())
                    .yieldVariancePct(r.getYieldVariancePct())
                    .yieldFlagged(Boolean.TRUE.equals(r.getYieldFlagged()))
                    .damagedPouches(r.getDamagedPouches())
                    .cartonsUsed(r.getCartonsUsed())
                    .labourCost(r.getLabourCost())
                    .overheadCost(r.getOverheadCost())
                    .calculatedUnitCost(r.getCalculatedUnitCost())
                    .rawMaterialConsumptions(cvList)
                    .build());
        }

        return ProductionReport.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRuns(runs.size())
                .totalOutputPackets(totalOutput)
                .totalLabourCost(totalLabour.setScale(2, RoundingMode.HALF_UP))
                .totalOverheadCost(totalOverhead.setScale(2, RoundingMode.HALF_UP))
                .runs(details)
                .build();
    }

    /**
     * 6. Payments & Receivables Ageing
     */
    @Transactional(readOnly = true)
    public ReceivablesAgeingReport getReceivablesReport(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> !p.getPaymentDate().isBefore(startDate) && !p.getPaymentDate().isAfter(endDate))
                .sorted(Comparator.comparing(Payment::getPaymentDate))
                .toList();

        BigDecimal collectedPeriod = payments.stream().map(Payment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReceivablesAgeingReport.PaymentCollected> paymentList = payments.stream().map(p -> ReceivablesAgeingReport.PaymentCollected.builder()
                .paymentId(p.getId())
                .invoiceId(p.getInvoice().getId())
                .invoiceNumber(p.getInvoice().getInvoiceNumber())
                .customerName(p.getInvoice().getCustomer().getName())
                .amountPaid(p.getAmountPaid())
                .paymentMode(p.getPaymentMode())
                .paymentDate(p.getPaymentDate())
                .build()).toList();

        List<Customer> customers = customerRepository.findAll();
        List<Invoice> activeInvoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .toList();

        BigDecimal totalInvoicedAll = BigDecimal.ZERO;
        BigDecimal totalOutstandingAll = BigDecimal.ZERO;
        List<ReceivablesAgeingReport.CustomerAgeing> customerAgeingList = new ArrayList<>();

        for (Customer c : customers) {
            List<Invoice> cInvoices = activeInvoices.stream()
                    .filter(i -> i.getCustomer().getId().equals(c.getId()))
                    .toList();

            BigDecimal cInvoiced = BigDecimal.ZERO;
            BigDecimal cPaid = BigDecimal.ZERO;
            BigDecimal b0to30 = BigDecimal.ZERO;
            BigDecimal b31to60 = BigDecimal.ZERO;
            BigDecimal b60Plus = BigDecimal.ZERO;

            for (Invoice inv : cInvoices) {
                cInvoiced = cInvoiced.add(inv.getGrandTotal());
                BigDecimal paidForInv = paymentRepository.sumAmountPaidByInvoiceId(inv.getId());
                if (paidForInv == null) paidForInv = BigDecimal.ZERO;
                cPaid = cPaid.add(paidForInv);

                BigDecimal due = inv.getGrandTotal().subtract(paidForInv).max(BigDecimal.ZERO);
                if (due.compareTo(BigDecimal.ZERO) > 0) {
                    long days = ChronoUnit.DAYS.between(inv.getInvoiceDate(), today);
                    if (days <= 30) {
                        b0to30 = b0to30.add(due);
                    } else if (days <= 60) {
                        b31to60 = b31to60.add(due);
                    } else {
                        b60Plus = b60Plus.add(due);
                    }
                }
            }

            BigDecimal cBalanceDue = cInvoiced.subtract(cPaid).max(BigDecimal.ZERO);
            totalInvoicedAll = totalInvoicedAll.add(cInvoiced);
            totalOutstandingAll = totalOutstandingAll.add(cBalanceDue);

            customerAgeingList.add(ReceivablesAgeingReport.CustomerAgeing.builder()
                    .customerId(c.getId())
                    .customerName(c.getName())
                    .gstin(c.getGstin())
                    .stateName(c.getStateName())
                    .totalInvoiced(cInvoiced.setScale(2, RoundingMode.HALF_UP))
                    .totalPaid(cPaid.setScale(2, RoundingMode.HALF_UP))
                    .balanceDue(cBalanceDue.setScale(2, RoundingMode.HALF_UP))
                    .bucket0to30(b0to30.setScale(2, RoundingMode.HALF_UP))
                    .bucket31to60(b31to60.setScale(2, RoundingMode.HALF_UP))
                    .bucket60Plus(b60Plus.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        return ReceivablesAgeingReport.builder()
                .asOfDate(today)
                .startDate(startDate)
                .endDate(endDate)
                .totalInvoicedAll(totalInvoicedAll.setScale(2, RoundingMode.HALF_UP))
                .totalCollectedPeriod(collectedPeriod.setScale(2, RoundingMode.HALF_UP))
                .totalOutstandingAll(totalOutstandingAll.setScale(2, RoundingMode.HALF_UP))
                .paymentsCollected(paymentList)
                .customerAgeing(customerAgeingList)
                .build();
    }
}
