package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.StockLedger;
import com.factoryflow.erp.domain.enums.MovementType;
import com.factoryflow.erp.exception.InsufficientStockException;
import com.factoryflow.erp.repository.StockLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for calculating the First-In, First-Out (FIFO) cost of issuing raw materials
 * and packaging components from inventory.
 *
 * <h3>FIFO Valuation Rules:</h3>
 * <ol>
 *   <li><strong>Inbound Layers:</strong> Positive inventory inflows originate from {@link MovementType#OPENING}
 *       and {@link MovementType#PURCHASE} movements in the {@code stock_ledger}, ordered chronologically by
 *       {@code created_at ASC, id ASC}. Each inbound entry defines an available layer with an initial quantity
 *       ({@code qty_change}) and acquisition cost ({@code unit_rate}).</li>
 *   <li><strong>Prior Outbound Depletion:</strong> Cumulative historical outbound consumptions
 *       ({@link MovementType#PROD_ISSUE} and {@link MovementType#PROD_WASTAGE}, where {@code qty_change < 0})
 *       deplete the oldest available inbound layers first. The absolute sum of these previous issues forms
 *       the offset into the inbound layer queue.</li>
 *   <li><strong>Current Issue Valuation:</strong> To issue a requested quantity ({@code issueQty}), the service
 *       identifies the remaining quantity in each layer after accounting for prior depletion and consumes from the
 *       oldest remaining layers sequentially until {@code issueQty} is completely satisfied. The layer cost is
 *       accumulated as {@code consumedLayerQty * layerUnitRate}.</li>
 *   <li><strong>Total FIFO Cost:</strong> The resulting total cost is rounded to 2 decimal places using
 *       {@link RoundingMode#HALF_UP}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class FifoValuationService {

    private final StockLedgerRepository stockLedgerRepository;

    private static final Set<MovementType> INBOUND_TYPES = Set.of(
            MovementType.OPENING,
            MovementType.PURCHASE
    );

    private static final Set<MovementType> OUTBOUND_TYPES = Set.of(
            MovementType.PROD_ISSUE,
            MovementType.PROD_WASTAGE
    );

    /**
     * Calculates the exact FIFO valuation cost for issuing {@code issueQty} of the given {@code itemId}.
     *
     * @param itemId   the item being issued
     * @param issueQty the quantity to issue (must be positive)
     * @return total FIFO acquisition cost rounded to 2 decimal places (HALF_UP)
     * @throws InsufficientStockException if requested issueQty exceeds available inventory
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateFifoCost(Long itemId, BigDecimal issueQty) {
        if (issueQty == null || issueQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Query chronological inbound layers (OPENING, PURCHASE)
        List<StockLedger> inboundLayers = stockLedgerRepository
                .findByItemIdAndMovementTypeInOrderByCreatedAtAscIdAsc(itemId, INBOUND_TYPES);

        // Sum historical outbound consumption (PROD_ISSUE + PROD_WASTAGE)
        BigDecimal totalOutboundNegative = stockLedgerRepository
                .sumQtyChangeByItemIdAndMovementTypeIn(itemId, OUTBOUND_TYPES);
        BigDecimal priorOutboundAbs = totalOutboundNegative.abs();

        BigDecimal cumulativeSkip = priorOutboundAbs;
        BigDecimal remainingToIssue = issueQty;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (StockLedger layer : inboundLayers) {
            BigDecimal layerQty = layer.getQtyChange();
            if (layerQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Check if this layer was already completely exhausted by prior outbound issues
            if (cumulativeSkip.compareTo(layerQty) >= 0) {
                cumulativeSkip = cumulativeSkip.subtract(layerQty);
                continue;
            }

            // Layer is partially or fully available
            BigDecimal availableInLayer = layerQty.subtract(cumulativeSkip);
            cumulativeSkip = BigDecimal.ZERO; // prior outbound accounted for

            BigDecimal takeFromLayer = remainingToIssue.min(availableInLayer);
            BigDecimal layerRate = layer.getUnitRate() != null ? layer.getUnitRate() : BigDecimal.ZERO;
            BigDecimal costForThisPortion = takeFromLayer.multiply(layerRate);
            totalCost = totalCost.add(costForThisPortion);

            remainingToIssue = remainingToIssue.subtract(takeFromLayer);

            if (remainingToIssue.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
        }

        if (remainingToIssue.compareTo(BigDecimal.ZERO) > 0) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for itemId %d to fulfill FIFO valuation. Shortage: %s",
                    itemId, remainingToIssue
            ));
        }

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }
}
