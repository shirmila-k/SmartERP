package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.Item;
import com.factoryflow.erp.domain.entity.Purchase;
import com.factoryflow.erp.domain.entity.StockLedger;
import com.factoryflow.erp.domain.entity.User;
import com.factoryflow.erp.domain.enums.MovementType;
import com.factoryflow.erp.dto.PurchaseRequest;
import com.factoryflow.erp.dto.PurchaseResponse;
import com.factoryflow.erp.exception.ResourceNotFoundException;
import com.factoryflow.erp.repository.ItemRepository;
import com.factoryflow.erp.repository.PurchaseRepository;
import com.factoryflow.erp.repository.StockLedgerRepository;
import com.factoryflow.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final StockLedgerRepository stockLedgerRepository;

    /**
     * Executes a raw material/packaging purchase inside a single @Transactional method:
     * 1. Computes totalAmount = qty.multiply(unitRate).setScale(2, RoundingMode.HALF_UP).
     * 2. Saves the Purchase record.
     * 3. Updates items.standard_rate to the latest unitRate.
     * 4. Inserts an immutable 'PURCHASE' entry into stock_ledger with positive qty_change = +qty.
     */
    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + request.getItemId()));

        User createdBy = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getCreatedBy()));

        BigDecimal qty = request.getQty().setScale(3, RoundingMode.HALF_UP);
        BigDecimal unitRate = request.getUnitRate().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = qty.multiply(unitRate).setScale(2, RoundingMode.HALF_UP);

        // 1. Save Purchase
        Purchase purchase = Purchase.builder()
                .supplierName(request.getSupplierName())
                .invoiceNo(request.getInvoiceNo())
                .purchaseDate(request.getPurchaseDate())
                .item(item)
                .qty(qty)
                .unitRate(unitRate)
                .totalAmount(totalAmount)
                .createdBy(createdBy)
                .build();
        purchase = purchaseRepository.save(purchase);

        // 2. Update item's standard_rate to latest unitRate
        item.setStandardRate(unitRate);
        itemRepository.save(item);

        // 3. Insert immutable 'PURCHASE' entry into stock_ledger
        StockLedger ledgerEntry = StockLedger.builder()
                .item(item)
                .movementType(MovementType.PURCHASE)
                .qtyChange(qty)
                .unitRate(unitRate)
                .sourceType("PURCHASE")
                .sourceId(purchase.getId())
                .remarks(String.format("Purchase from %s, Inv: %s", request.getSupplierName(), request.getInvoiceNo()))
                .build();
        stockLedgerRepository.save(ledgerEntry);

        return mapToResponse(purchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getAllPurchases() {
        return purchaseRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PurchaseResponse mapToResponse(Purchase purchase) {
        return PurchaseResponse.builder()
                .id(purchase.getId())
                .supplierName(purchase.getSupplierName())
                .invoiceNo(purchase.getInvoiceNo())
                .purchaseDate(purchase.getPurchaseDate())
                .itemId(purchase.getItem().getId())
                .skuCode(purchase.getItem().getSkuCode())
                .itemName(purchase.getItem().getName())
                .unit(purchase.getItem().getUnit())
                .qty(purchase.getQty())
                .unitRate(purchase.getUnitRate())
                .totalAmount(purchase.getTotalAmount())
                .createdById(purchase.getCreatedBy().getId())
                .createdByUsername(purchase.getCreatedBy().getUsername())
                .createdAt(purchase.getCreatedAt())
                .build();
    }
}
