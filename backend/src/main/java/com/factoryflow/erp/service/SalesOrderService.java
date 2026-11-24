package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.Customer;
import com.factoryflow.erp.domain.entity.Item;
import com.factoryflow.erp.domain.entity.SalesOrder;
import com.factoryflow.erp.domain.entity.SalesOrderLine;
import com.factoryflow.erp.domain.entity.User;
import com.factoryflow.erp.domain.enums.SalesOrderStatus;
import com.factoryflow.erp.dto.SalesOrderRequest;
import com.factoryflow.erp.dto.SalesOrderResponse;
import com.factoryflow.erp.exception.ResourceNotFoundException;
import com.factoryflow.erp.repository.CustomerRepository;
import com.factoryflow.erp.repository.ItemRepository;
import com.factoryflow.erp.repository.SalesOrderRepository;
import com.factoryflow.erp.repository.StockLedgerRepository;
import com.factoryflow.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final StockLedgerRepository stockLedgerRepository;

    /**
     * Creates a Sales Order:
     * - Generates sequential orderNumber (ORD-YYYY-XXXX).
     * - Checks live available stock (SUM(qty_change)) for each ordered item.
     * - If orderedQty > availableStock, saves order with status = SUBMITTED,
     *   stockShortageFlagged = true, and shortageQty = (orderedQty - availableStock).
     */
    @Transactional
    public SalesOrderResponse createSalesOrder(SalesOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        User salesman = userRepository.findById(request.getSalesmanId())
                .orElseThrow(() -> new ResourceNotFoundException("Salesman not found with id: " + request.getSalesmanId()));

        // Sequential order number
        long count = salesOrderRepository.count() + 1;
        int currentYear = Year.now().getValue();
        String orderNumber = String.format("ORD-%d-%04d", currentYear, count);

        boolean anyShortage = false;
        int totalShortage = 0;

        List<SalesOrderLine> lines = new ArrayList<>();

        SalesOrder order = SalesOrder.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .salesman(salesman)
                .status(SalesOrderStatus.SUBMITTED)
                .stockShortageFlagged(false)
                .shortageQty(0)
                .orderDate(LocalDateTime.now())
                .build();

        for (SalesOrderRequest.OrderLineRequest lineReq : request.getLines()) {
            Item item = itemRepository.findById(lineReq.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + lineReq.getItemId()));

            BigDecimal unitPrice = lineReq.getUnitPrice() != null
                    ? lineReq.getUnitPrice().setScale(2, RoundingMode.HALF_UP)
                    : (item.getSellingPrice() != null ? item.getSellingPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

            // Check live stock
            BigDecimal stock = stockLedgerRepository.findBalanceByItemId(item.getId());
            int available = stock.intValue();
            int ordered = lineReq.getQty();

            if (ordered > available) {
                anyShortage = true;
                int diff = ordered - Math.max(0, available);
                totalShortage += diff;
            }

            SalesOrderLine orderLine = SalesOrderLine.builder()
                    .order(order)
                    .item(item)
                    .qty(ordered)
                    .unitPrice(unitPrice)
                    .build();
            lines.add(orderLine);
        }

        order.setStockShortageFlagged(anyShortage);
        order.setShortageQty(totalShortage);
        order.setLines(lines);

        SalesOrder saved = salesOrderRepository.save(order);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SalesOrderResponse> getAllOrders() {
        return salesOrderRepository.findAllByOrderByOrderDateDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesOrderResponse> getOrdersBySalesman(Long salesmanId) {
        return salesOrderRepository.findBySalesmanIdOrderByOrderDateDesc(salesmanId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getOrderById(Long orderId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found with id: " + orderId));
        return mapToResponse(order);
    }

    private SalesOrderResponse mapToResponse(SalesOrder order) {
        BigDecimal totalEstimated = BigDecimal.ZERO;

        List<SalesOrderResponse.SalesOrderLineDto> lineDtos = new ArrayList<>();
        if (order.getLines() != null) {
            for (SalesOrderLine line : order.getLines()) {
                BigDecimal lineTotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty()))
                        .setScale(2, RoundingMode.HALF_UP);
                totalEstimated = totalEstimated.add(lineTotal);

                lineDtos.add(SalesOrderResponse.SalesOrderLineDto.builder()
                        .id(line.getId())
                        .itemId(line.getItem().getId())
                        .skuCode(line.getItem().getSkuCode())
                        .itemName(line.getItem().getName())
                        .qty(line.getQty())
                        .unitPrice(line.getUnitPrice())
                        .lineTotal(lineTotal)
                        .build());
            }
        }

        return SalesOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .customerState(order.getCustomer().getStateName())
                .customerGstin(order.getCustomer().getGstin())
                .salesmanId(order.getSalesman().getId())
                .salesmanName(order.getSalesman().getFullName())
                .status(order.getStatus())
                .stockShortageFlagged(Boolean.TRUE.equals(order.getStockShortageFlagged()))
                .shortageQty(order.getShortageQty())
                .orderDate(order.getOrderDate())
                .totalEstimatedAmount(totalEstimated.setScale(2, RoundingMode.HALF_UP))
                .lines(lineDtos)
                .build();
    }
}
