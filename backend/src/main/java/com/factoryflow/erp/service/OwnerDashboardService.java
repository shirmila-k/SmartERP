package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.Item;
import com.factoryflow.erp.domain.entity.ProductionRun;
import com.factoryflow.erp.domain.entity.SalesOrder;
import com.factoryflow.erp.domain.enums.ItemType;
import com.factoryflow.erp.dto.dashboard.OwnerDashboardResponse;
import com.factoryflow.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerDashboardService {

    private final SalesOrderRepository salesOrderRepository;
    private final ItemRepository itemRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final ProductionRunRepository productionRunRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getOwnerDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        // 1. Today's orders
        List<SalesOrder> allOrders = salesOrderRepository.findAll();
        List<SalesOrder> todaysOrders = allOrders.stream()
                .filter(o -> !o.getOrderDate().isBefore(todayStart))
                .toList();

        int todaysOrdersCount = todaysOrders.size();
        BigDecimal todaysOrdersValue = todaysOrders.stream()
                .flatMap(o -> o.getLines().stream())
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 2. Finished goods stock & Low stock alerts
        List<Item> allItems = itemRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<OwnerDashboardResponse.FinishedGoodStockDto> fgList = new ArrayList<>();
        List<OwnerDashboardResponse.LowStockAlertDto> lowStockList = new ArrayList<>();

        for (Item item : allItems) {
            BigDecimal bal = stockLedgerRepository.findBalanceByItemId(item.getId());
            if (bal == null) bal = BigDecimal.ZERO;
            bal = bal.setScale(3, RoundingMode.HALF_UP);

            BigDecimal reorder = item.getReorderLevel().setScale(3, RoundingMode.HALF_UP);
            boolean belowReorder = bal.compareTo(reorder) <= 0;

            if (item.getItemType() == ItemType.FINISHED_GOOD) {
                fgList.add(OwnerDashboardResponse.FinishedGoodStockDto.builder()
                        .itemId(item.getId())
                        .skuCode(item.getSkuCode())
                        .name(item.getName())
                        .currentBalance(bal)
                        .reorderLevel(reorder)
                        .belowReorderLevel(belowReorder)
                        .build());
            }

            if (belowReorder) {
                BigDecimal shortage = reorder.subtract(bal).max(BigDecimal.ZERO);
                lowStockList.add(OwnerDashboardResponse.LowStockAlertDto.builder()
                        .itemId(item.getId())
                        .skuCode(item.getSkuCode())
                        .name(item.getName())
                        .itemType(item.getItemType().name())
                        .unit(item.getUnit().name())
                        .currentBalance(bal)
                        .reorderLevel(reorder)
                        .shortage(shortage)
                        .build());
            }
        }

        // 3. Active variance alerts
        List<ProductionRun> recentRuns = productionRunRepository.findAll(Sort.by(Sort.Direction.DESC, "runDate", "id"));
        List<OwnerDashboardResponse.ActiveVarianceAlertDto> varianceAlerts = new ArrayList<>();

        for (ProductionRun r : recentRuns) {
            boolean runFlagged = Boolean.TRUE.equals(r.getYieldFlagged());
            List<String> flaggedLines = r.getConsumptions().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getVarianceFlagged()))
                    .map(c -> String.format("%s: %s%%", c.getItem().getName(), c.getVariancePct()))
                    .toList();

            if (runFlagged || !flaggedLines.isEmpty()) {
                varianceAlerts.add(OwnerDashboardResponse.ActiveVarianceAlertDto.builder()
                        .runId(r.getId())
                        .runCode(r.getRunCode())
                        .runDate(r.getRunDate())
                        .productName(r.getFinishedItem().getName())
                        .yieldVariancePct(r.getYieldVariancePct())
                        .yieldFlagged(runFlagged)
                        .flaggedConsumptions(flaggedLines)
                        .build());
            }
        }

        // 4. Latest cost per packet
        OwnerDashboardResponse.LatestCostPerPacketDto latestCostDto = null;
        if (!recentRuns.isEmpty()) {
            ProductionRun latest = recentRuns.get(0);
            Item fg = latest.getFinishedItem();
            BigDecimal cost = latest.getCalculatedUnitCost();
            BigDecimal sellingPrice = fg.getSellingPrice() != null ? fg.getSellingPrice() : BigDecimal.ZERO;
            BigDecimal margin = BigDecimal.ZERO;
            if (sellingPrice.compareTo(BigDecimal.ZERO) > 0) {
                margin = sellingPrice.subtract(cost).divide(sellingPrice, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            }

            latestCostDto = OwnerDashboardResponse.LatestCostPerPacketDto.builder()
                    .runId(latest.getId())
                    .runCode(latest.getRunCode())
                    .productName(fg.getName())
                    .calculatedUnitCost(cost)
                    .sellingPrice(sellingPrice)
                    .grossMarginPct(margin)
                    .build();
        }

        // 5. Total outstanding dues
        BigDecimal totalInvoiced = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus().name().equals("ISSUED") || i.getStatus().name().equals("DISPATCHED"))
                .map(i -> i.getGrandTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = paymentRepository.findAll().stream()
                .map(p -> p.getAmountPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = totalInvoiced.subtract(totalPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return OwnerDashboardResponse.builder()
                .todaysOrdersCount(todaysOrdersCount)
                .todaysOrdersValue(todaysOrdersValue)
                .finishedGoodsStock(fgList)
                .lowStockAlerts(lowStockList)
                .activeVarianceAlerts(varianceAlerts)
                .latestCostPerPacket(latestCostDto)
                .totalOutstandingDues(totalOutstanding)
                .build();
    }
}
