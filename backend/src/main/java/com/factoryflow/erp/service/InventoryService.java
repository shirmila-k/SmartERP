package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.Item;
import com.factoryflow.erp.dto.InventoryBalanceResponse;
import com.factoryflow.erp.repository.ItemRepository;
import com.factoryflow.erp.repository.StockLedgerRepository;
import com.factoryflow.erp.repository.projection.StockBalanceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ItemRepository itemRepository;
    private final StockLedgerRepository stockLedgerRepository;

    /**
     * Computes current stock balances for all items strictly from SUM(qty_change) in stock_ledger.
     */
    @Transactional(readOnly = true)
    public List<InventoryBalanceResponse> getAllInventoryBalances() {
        Map<Long, BigDecimal> balanceByItemId = stockLedgerRepository.findStockBalancesGroupedByItem()
                .stream()
                .collect(Collectors.toMap(
                        StockBalanceProjection::getItemId,
                        p -> p.getBalance() != null ? p.getBalance() : BigDecimal.ZERO
                ));

        List<Item> items = itemRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        return items.stream()
                .map(item -> toBalanceResponse(item, balanceByItemId.getOrDefault(item.getId(), BigDecimal.ZERO)))
                .toList();
    }

    private InventoryBalanceResponse toBalanceResponse(Item item, BigDecimal rawBalance) {
        BigDecimal balance = rawBalance.setScale(3, RoundingMode.HALF_UP);
        BigDecimal reorderLevel = item.getReorderLevel().setScale(3, RoundingMode.HALF_UP);
        BigDecimal standardRate = item.getStandardRate().setScale(2, RoundingMode.HALF_UP);
        BigDecimal stockValue = balance.multiply(standardRate).setScale(2, RoundingMode.HALF_UP);

        return InventoryBalanceResponse.builder()
                .itemId(item.getId())
                .skuCode(item.getSkuCode())
                .name(item.getName())
                .itemType(item.getItemType())
                .unit(item.getUnit())
                .currentBalance(balance)
                .reorderLevel(reorderLevel)
                .belowReorderLevel(balance.compareTo(reorderLevel) < 0)
                .standardRate(standardRate)
                .stockValue(stockValue)
                .build();
    }
}
