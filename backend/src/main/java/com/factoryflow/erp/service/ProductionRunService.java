package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.*;
import com.factoryflow.erp.domain.enums.MovementType;
import com.factoryflow.erp.dto.BomResponse;
import com.factoryflow.erp.dto.ProductionRunRequest;
import com.factoryflow.erp.dto.ProductionRunResponse;
import com.factoryflow.erp.exception.InsufficientStockException;
import com.factoryflow.erp.exception.ResourceNotFoundException;
import com.factoryflow.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionRunService {

    private final ProductionRunRepository productionRunRepository;
    private final ProductionConsumptionRepository productionConsumptionRepository;
    private final BomHeaderRepository bomHeaderRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final AuditLogRepository auditLogRepository;
    private final FifoValuationService fifoValuationService;

    private static final BigDecimal VARIANCE_THRESHOLD = new BigDecimal("3.00");

    /**
     * Executes a BOM production run adhering to strict ACID constraints:
     * 1. Validates live stock balances for all raw materials, pouches, and cartons.
     * 2. Calculates expected packets, yield variance %, and flags >3% variances.
     * 3. Calculates raw material consumption variances and flags >3% variances.
     * 4. Issues stock and captures FIFO valuation costs.
     * 5. Issues packaging and records damaged pouches as PROD_WASTAGE.
     * 6. Outputs finished goods and calculates unit cost per packet.
     * 7. Updates finishedItem standard_rate and logs an audit record.
     */
    @Transactional
    public ProductionRunResponse createProductionRun(ProductionRunRequest request) {
        Item finishedItem = itemRepository.findById(request.getFinishedItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Finished item not found: " + request.getFinishedItemId()));

        BomHeader bom = bomHeaderRepository.findByFinishedItemId(finishedItem.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No BOM defined for item: " + finishedItem.getSkuCode()));

        User createdBy = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getCreatedBy()));

        BigDecimal batchesPlanned = request.getBatchesPlanned().setScale(2, RoundingMode.HALF_UP);
        Integer actualGoodPackets = request.getActualGoodPackets();
        Integer damagedPouches = request.getDamagedPouches() != null ? request.getDamagedPouches() : 0;
        Integer cartonsUsed = request.getCartonsUsed() != null ? request.getCartonsUsed() : 0;
        BigDecimal labourCost = request.getLabourCost().setScale(2, RoundingMode.HALF_UP);
        BigDecimal overheadCost = request.getOverheadCost().setScale(2, RoundingMode.HALF_UP);

        // Classify BOM lines
        List<BomLine> bomLines = bom.getLines();
        List<BomLine> rawBomLines = bomLines.stream().filter(l -> !Boolean.TRUE.equals(l.getIsPackaging())).toList();
        List<BomLine> packagingBomLines = bomLines.stream().filter(l -> Boolean.TRUE.equals(l.getIsPackaging())).toList();

        BomLine pouchLine = packagingBomLines.stream()
                .filter(l -> l.getComponentItem().getSkuCode().toUpperCase().contains("POUCH"))
                .findFirst()
                .orElse(null);

        BomLine cartonLine = packagingBomLines.stream()
                .filter(l -> l.getComponentItem().getSkuCode().toUpperCase().contains("CARTON"))
                .findFirst()
                .orElse(null);

        // Map submitted raw material actual quantities by itemId
        Map<Long, BigDecimal> rawActualQtyMap = request.getRawConsumptions().stream()
                .collect(Collectors.toMap(
                        ProductionRunRequest.RawConsumptionItem::getItemId,
                        c -> c.getActualQty().setScale(3, RoundingMode.HALF_UP)
                ));

        // 1. Stock Availability Check
        // Check raw materials
        for (BomLine line : rawBomLines) {
            Long rawId = line.getComponentItem().getId();
            BigDecimal actualQty = rawActualQtyMap.get(rawId);
            if (actualQty == null) {
                throw new IllegalArgumentException("Missing raw consumption for item: " + line.getComponentItem().getSkuCode());
            }
            BigDecimal currentBalance = stockLedgerRepository.findBalanceByItemId(rawId);
            if (currentBalance.compareTo(actualQty) < 0) {
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for raw material %s. Available: %s, Required: %s",
                        line.getComponentItem().getSkuCode(), currentBalance, actualQty
                ));
            }
        }

        // Check pouches stock: (actualGoodPackets + damagedPouches)
        BigDecimal totalPouchesNeeded = BigDecimal.valueOf(actualGoodPackets + damagedPouches).setScale(3, RoundingMode.HALF_UP);
        if (pouchLine != null && totalPouchesNeeded.compareTo(BigDecimal.ZERO) > 0) {
            Long pouchId = pouchLine.getComponentItem().getId();
            BigDecimal pouchBalance = stockLedgerRepository.findBalanceByItemId(pouchId);
            if (pouchBalance.compareTo(totalPouchesNeeded) < 0) {
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for pouches %s. Available: %s, Required: %s",
                        pouchLine.getComponentItem().getSkuCode(), pouchBalance, totalPouchesNeeded
                ));
            }
        }

        // Check cartons stock: cartonsUsed
        BigDecimal totalCartonsNeeded = BigDecimal.valueOf(cartonsUsed).setScale(3, RoundingMode.HALF_UP);
        if (cartonLine != null && totalCartonsNeeded.compareTo(BigDecimal.ZERO) > 0) {
            Long cartonId = cartonLine.getComponentItem().getId();
            BigDecimal cartonBalance = stockLedgerRepository.findBalanceByItemId(cartonId);
            if (cartonBalance.compareTo(totalCartonsNeeded) < 0) {
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for cartons %s. Available: %s, Required: %s",
                        cartonLine.getComponentItem().getSkuCode(), cartonBalance, totalCartonsNeeded
                ));
            }
        }

        // 2. Expected Output & Yield Variance
        // expectedOutputPackets = bomHeader.expectedYieldPackets * batchesPlanned
        BigDecimal expectedOutputBd = BigDecimal.valueOf(bom.getExpectedYieldPackets()).multiply(batchesPlanned);
        int expectedOutputPackets = expectedOutputBd.intValue();

        // yieldVariancePct = ((actualGoodPackets - expectedOutputPackets) / expectedOutputPackets) * 100
        BigDecimal actualGoodBd = BigDecimal.valueOf(actualGoodPackets);
        BigDecimal expectedBd = BigDecimal.valueOf(expectedOutputPackets);
        BigDecimal yieldDiff = actualGoodBd.subtract(expectedBd);
        BigDecimal yieldVariancePct = yieldDiff.divide(expectedBd, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        boolean yieldFlagged = yieldVariancePct.abs().compareTo(VARIANCE_THRESHOLD) > 0;

        // Generate run code if not provided
        String runCode = request.getRunCode();
        if (runCode == null || runCode.isBlank()) {
            runCode = "PR-" + System.currentTimeMillis();
        }

        // Initialize ProductionRun entity
        ProductionRun productionRun = ProductionRun.builder()
                .runCode(runCode)
                .finishedItem(finishedItem)
                .batchesPlanned(batchesPlanned)
                .expectedOutputPackets(expectedOutputPackets)
                .actualGoodPackets(actualGoodPackets)
                .damagedPouches(damagedPouches)
                .cartonsUsed(cartonsUsed)
                .labourCost(labourCost)
                .overheadCost(overheadCost)
                .yieldVariancePct(yieldVariancePct)
                .yieldFlagged(yieldFlagged)
                .calculatedUnitCost(BigDecimal.ZERO) // computed below
                .runDate(request.getRunDate())
                .createdBy(createdBy)
                .build();
        productionRun = productionRunRepository.save(productionRun);

        // 3. Raw Material Consumption & FIFO Cost
        BigDecimal totalRawMaterialFifoCost = BigDecimal.ZERO;
        List<ProductionConsumption> consumptions = new ArrayList<>();

        for (BomLine line : rawBomLines) {
            Item rawItem = line.getComponentItem();
            BigDecimal plannedQty = line.getStandardQty().multiply(batchesPlanned).setScale(3, RoundingMode.HALF_UP);
            BigDecimal actualQty = rawActualQtyMap.get(rawItem.getId());

            // variancePct = ((actualQty - plannedQty) / plannedQty) * 100
            BigDecimal qtyDiff = actualQty.subtract(plannedQty);
            BigDecimal variancePct = qtyDiff.divide(plannedQty, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);

            boolean varianceFlagged = variancePct.abs().compareTo(VARIANCE_THRESHOLD) > 0;

            // FIFO Valuation for actualQty
            BigDecimal rawFifoCost = fifoValuationService.calculateFifoCost(rawItem.getId(), actualQty);
            totalRawMaterialFifoCost = totalRawMaterialFifoCost.add(rawFifoCost);

            BigDecimal effectiveUnitCost = actualQty.compareTo(BigDecimal.ZERO) > 0
                    ? rawFifoCost.divide(actualQty, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            ProductionConsumption pc = ProductionConsumption.builder()
                    .productionRun(productionRun)
                    .item(rawItem)
                    .plannedQty(plannedQty)
                    .actualQty(actualQty)
                    .variancePct(variancePct)
                    .varianceFlagged(varianceFlagged)
                    .unitCostApplied(effectiveUnitCost)
                    .build();
            consumptions.add(productionConsumptionRepository.save(pc));

            // Stock Ledger Entry: negative PROD_ISSUE
            StockLedger rawIssue = StockLedger.builder()
                    .item(rawItem)
                    .movementType(MovementType.PROD_ISSUE)
                    .qtyChange(actualQty.negate())
                    .unitRate(effectiveUnitCost)
                    .sourceType("PRODUCTION_RUN")
                    .sourceId(productionRun.getId())
                    .remarks(String.format("Production run %s raw material issue", runCode))
                    .build();
            stockLedgerRepository.save(rawIssue);
        }

        // 4. Packaging Consumption & Wastage
        BigDecimal totalPackagingFifoCost = BigDecimal.ZERO;

        // Pouches: good pouches + damaged pouches
        if (pouchLine != null && totalPouchesNeeded.compareTo(BigDecimal.ZERO) > 0) {
            Item pouchItem = pouchLine.getComponentItem();
            BigDecimal pouchFifoCost = fifoValuationService.calculateFifoCost(pouchItem.getId(), totalPouchesNeeded);
            totalPackagingFifoCost = totalPackagingFifoCost.add(pouchFifoCost);

            BigDecimal pouchUnitCost = pouchFifoCost.divide(totalPouchesNeeded, 2, RoundingMode.HALF_UP);

            // Good pouches issue
            BigDecimal goodPouchesQty = BigDecimal.valueOf(actualGoodPackets).setScale(3, RoundingMode.HALF_UP);
            StockLedger pouchIssue = StockLedger.builder()
                    .item(pouchItem)
                    .movementType(MovementType.PROD_ISSUE)
                    .qtyChange(goodPouchesQty.negate())
                    .unitRate(pouchUnitCost)
                    .sourceType("PRODUCTION_RUN")
                    .sourceId(productionRun.getId())
                    .remarks(String.format("Production run %s good pouches used", runCode))
                    .build();
            stockLedgerRepository.save(pouchIssue);

            // Damaged pouches wastage
            if (damagedPouches > 0) {
                BigDecimal damagedQty = BigDecimal.valueOf(damagedPouches).setScale(3, RoundingMode.HALF_UP);
                StockLedger pouchWastage = StockLedger.builder()
                        .item(pouchItem)
                        .movementType(MovementType.PROD_WASTAGE)
                        .qtyChange(damagedQty.negate())
                        .unitRate(pouchUnitCost)
                        .sourceType("PRODUCTION_RUN")
                        .sourceId(productionRun.getId())
                        .remarks(String.format("Production run %s damaged pouches wastage", runCode))
                        .build();
                stockLedgerRepository.save(pouchWastage);
            }
        }

        // Cartons
        if (cartonLine != null && totalCartonsNeeded.compareTo(BigDecimal.ZERO) > 0) {
            Item cartonItem = cartonLine.getComponentItem();
            BigDecimal cartonFifoCost = fifoValuationService.calculateFifoCost(cartonItem.getId(), totalCartonsNeeded);
            totalPackagingFifoCost = totalPackagingFifoCost.add(cartonFifoCost);

            BigDecimal cartonUnitCost = cartonFifoCost.divide(totalCartonsNeeded, 2, RoundingMode.HALF_UP);

            StockLedger cartonIssue = StockLedger.builder()
                    .item(cartonItem)
                    .movementType(MovementType.PROD_ISSUE)
                    .qtyChange(totalCartonsNeeded.negate())
                    .unitRate(cartonUnitCost)
                    .sourceType("PRODUCTION_RUN")
                    .sourceId(productionRun.getId())
                    .remarks(String.format("Production run %s cartons used", runCode))
                    .build();
            stockLedgerRepository.save(cartonIssue);
        }

        // 5. Finished Goods Output & Unit Cost Math
        // totalBatchCost = totalRawMaterialFifoCost + totalPackagingFifoCost + labourCost + overheadCost
        BigDecimal totalBatchCost = totalRawMaterialFifoCost
                .add(totalPackagingFifoCost)
                .add(labourCost)
                .add(overheadCost)
                .setScale(2, RoundingMode.HALF_UP);

        // calculatedUnitCost = totalBatchCost / actualGoodPackets
        BigDecimal calculatedUnitCost = totalBatchCost.divide(BigDecimal.valueOf(actualGoodPackets), 2, RoundingMode.HALF_UP);

        // Insert positive 'PROD_OUTPUT' row in stock_ledger
        StockLedger fgOutput = StockLedger.builder()
                .item(finishedItem)
                .movementType(MovementType.PROD_OUTPUT)
                .qtyChange(BigDecimal.valueOf(actualGoodPackets).setScale(3, RoundingMode.HALF_UP))
                .unitRate(calculatedUnitCost)
                .sourceType("PRODUCTION_RUN")
                .sourceId(productionRun.getId())
                .remarks(String.format("Production run %s output", runCode))
                .build();
        stockLedgerRepository.save(fgOutput);

        // Update production run with calculatedUnitCost
        productionRun.setCalculatedUnitCost(calculatedUnitCost);
        productionRun.setConsumptions(consumptions);
        productionRun = productionRunRepository.save(productionRun);

        // Update finished item standard rate
        finishedItem.setStandardRate(calculatedUnitCost);
        itemRepository.save(finishedItem);

        // Audit Log
        AuditLog auditLog = AuditLog.builder()
                .entityType("PRODUCTION_RUN")
                .entityId(productionRun.getId())
                .action("COMPLETE_RUN")
                .changedBy(createdBy)
                .details(String.format(
                        "Run %s completed. Output: %d packets, Total batch cost: %s, Calculated unit cost: %s",
                        runCode, actualGoodPackets, totalBatchCost, calculatedUnitCost
                ))
                .build();
        auditLogRepository.save(auditLog);

        return mapToResponse(productionRun, totalRawMaterialFifoCost, totalPackagingFifoCost, totalBatchCost);
    }

    @Transactional(readOnly = true)
    public List<ProductionRunResponse> getAllProductionRuns() {
        return productionRunRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(run -> {
                    BigDecimal rawCost = run.getConsumptions().stream()
                            .map(c -> c.getActualQty().multiply(c.getUnitCostApplied()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal totalBatch = run.getCalculatedUnitCost()
                            .multiply(BigDecimal.valueOf(run.getActualGoodPackets()))
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal pkgCost = totalBatch.subtract(rawCost).subtract(run.getLabourCost()).subtract(run.getOverheadCost());
                    return mapToResponse(run, rawCost, pkgCost.max(BigDecimal.ZERO), totalBatch);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BomResponse getBomByFinishedItemId(Long finishedItemId) {
        BomHeader bom = bomHeaderRepository.findByFinishedItemId(finishedItemId)
                .orElseThrow(() -> new ResourceNotFoundException("No BOM found for finishedItemId: " + finishedItemId));

        List<BomResponse.BomLineDto> lines = bom.getLines().stream()
                .map(l -> BomResponse.BomLineDto.builder()
                        .id(l.getId())
                        .componentItemId(l.getComponentItem().getId())
                        .componentSkuCode(l.getComponentItem().getSkuCode())
                        .componentName(l.getComponentItem().getName())
                        .componentType(l.getComponentItem().getItemType())
                        .unit(l.getComponentItem().getUnit())
                        .standardQty(l.getStandardQty())
                        .isPackaging(Boolean.TRUE.equals(l.getIsPackaging()))
                        .build())
                .toList();

        return BomResponse.builder()
                .bomId(bom.getId())
                .finishedItemId(bom.getFinishedItem().getId())
                .finishedItemSku(bom.getFinishedItem().getSkuCode())
                .finishedItemName(bom.getFinishedItem().getName())
                .recipeGroup(bom.getRecipeGroup())
                .expectedYieldPackets(bom.getExpectedYieldPackets())
                .lines(lines)
                .build();
    }

    private ProductionRunResponse mapToResponse(
            ProductionRun run,
            BigDecimal rawCost,
            BigDecimal pkgCost,
            BigDecimal totalBatchCost) {

        List<ProductionRunResponse.ConsumptionLineDto> consumptionDtos = run.getConsumptions().stream()
                .map(c -> ProductionRunResponse.ConsumptionLineDto.builder()
                        .id(c.getId())
                        .itemId(c.getItem().getId())
                        .skuCode(c.getItem().getSkuCode())
                        .itemName(c.getItem().getName())
                        .unit(c.getItem().getUnit())
                        .plannedQty(c.getPlannedQty())
                        .actualQty(c.getActualQty())
                        .variancePct(c.getVariancePct())
                        .varianceFlagged(Boolean.TRUE.equals(c.getVarianceFlagged()))
                        .unitCostApplied(c.getUnitCostApplied())
                        .totalFifoCost(c.getActualQty().multiply(c.getUnitCostApplied()).setScale(2, RoundingMode.HALF_UP))
                        .build())
                .toList();

        return ProductionRunResponse.builder()
                .id(run.getId())
                .runCode(run.getRunCode())
                .finishedItemId(run.getFinishedItem().getId())
                .finishedItemSku(run.getFinishedItem().getSkuCode())
                .finishedItemName(run.getFinishedItem().getName())
                .batchesPlanned(run.getBatchesPlanned())
                .expectedOutputPackets(run.getExpectedOutputPackets())
                .actualGoodPackets(run.getActualGoodPackets())
                .damagedPouches(run.getDamagedPouches())
                .cartonsUsed(run.getCartonsUsed())
                .labourCost(run.getLabourCost())
                .overheadCost(run.getOverheadCost())
                .totalRawMaterialCost(rawCost)
                .totalPackagingCost(pkgCost)
                .totalBatchCost(totalBatchCost)
                .yieldVariancePct(run.getYieldVariancePct())
                .yieldFlagged(Boolean.TRUE.equals(run.getYieldFlagged()))
                .calculatedUnitCost(run.getCalculatedUnitCost())
                .runDate(run.getRunDate())
                .createdById(run.getCreatedBy().getId())
                .createdByUsername(run.getCreatedBy().getUsername())
                .createdAt(run.getCreatedAt())
                .consumptions(consumptionDtos)
                .build();
    }
}
