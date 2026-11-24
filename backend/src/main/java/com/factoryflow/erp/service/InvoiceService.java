package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.*;
import com.factoryflow.erp.domain.enums.InvoiceStatus;
import com.factoryflow.erp.domain.enums.MovementType;
import com.factoryflow.erp.domain.enums.SalesOrderStatus;
import com.factoryflow.erp.dto.ApproveOrderRequest;
import com.factoryflow.erp.dto.InvoiceResponse;
import com.factoryflow.erp.dto.SalesOrderResponse;
import com.factoryflow.erp.exception.InsufficientStockException;
import com.factoryflow.erp.exception.ResourceNotFoundException;
import com.factoryflow.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final AuditLogRepository auditLogRepository;
    private final ItemRepository itemRepository;

    private static final String FACTORY_STATE = "Tamil Nadu";
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    /**
     * Approves order & generates GST tax invoice:
     * - Admin can pass optional line adjustments.
     * - Re-verifies live stock covers approved quantities.
     * - Place of Supply rule (Factory in Tamil Nadu):
     *   - If customer.stateName == Tamil Nadu -> CGST + SGST (totalTax split equally), IGST = 0.
     *   - Else -> IGST = totalTax, CGST = 0, SGST = 0.
     * - Sequential FY Invoice Number (INV/26-27/0001).
     * - Sets order.status = INVOICED, invoice.status = ISSUED.
     */
    @Transactional
    public InvoiceResponse approveAndGenerateInvoice(Long orderId, ApproveOrderRequest request) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found with id: " + orderId));

        if (order.getStatus() != SalesOrderStatus.SUBMITTED) {
            throw new IllegalArgumentException("Cannot approve order with status: " + order.getStatus());
        }

        // Apply adjustments if provided
        if (request != null && request.getAdjustedLines() != null && !request.getAdjustedLines().isEmpty()) {
            for (ApproveOrderRequest.AdjustedLine adj : request.getAdjustedLines()) {
                for (SalesOrderLine line : order.getLines()) {
                    if (line.getItem().getId().equals(adj.getItemId())) {
                        if (adj.getQty() != null && adj.getQty() > 0) {
                            line.setQty(adj.getQty());
                        }
                        if (adj.getUnitPrice() != null && adj.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                            line.setUnitPrice(adj.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
                        }
                    }
                }
            }
        }

        // Verify live stock covers approved quantities
        for (SalesOrderLine line : order.getLines()) {
            BigDecimal stock = stockLedgerRepository.findBalanceByItemId(line.getItem().getId());
            if (stock.compareTo(BigDecimal.valueOf(line.getQty())) < 0) {
                int shortage = BigDecimal.valueOf(line.getQty()).subtract(stock).intValue();
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for item %s (%s). Required: %d, Available: %s, Shortage: %d",
                        line.getItem().getSkuCode(), line.getItem().getName(), line.getQty(), stock, shortage
                ));
            }
        }

        Customer customer = order.getCustomer();
        boolean isIntraState = customer.getStateName() != null &&
                customer.getStateName().trim().equalsIgnoreCase(FACTORY_STATE);

        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;

        for (SalesOrderLine line : order.getLines()) {
            BigDecimal lineQty = BigDecimal.valueOf(line.getQty());
            BigDecimal lineTaxable = lineQty.multiply(line.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            taxableAmount = taxableAmount.add(lineTaxable);

            BigDecimal gstRate = line.getItem().getGstRate() != null ? line.getItem().getGstRate() : BigDecimal.ZERO;
            BigDecimal lineTax = lineTaxable.multiply(gstRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            if (isIntraState) {
                BigDecimal halfTax = lineTax.divide(TWO, 2, RoundingMode.HALF_UP);
                cgstAmount = cgstAmount.add(halfTax);
                sgstAmount = sgstAmount.add(halfTax);
            } else {
                igstAmount = igstAmount.add(lineTax);
            }
        }

        BigDecimal grandTotal = taxableAmount.add(cgstAmount).add(sgstAmount).add(igstAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // Sequential FY invoice number: INV/26-27/0001
        long invCount = invoiceRepository.count() + 1;
        String invoiceNumber = String.format("INV/26-27/%04d", invCount);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .customer(customer)
                .taxableAmount(taxableAmount)
                .cgstAmount(cgstAmount)
                .sgstAmount(sgstAmount)
                .igstAmount(igstAmount)
                .grandTotal(grandTotal)
                .status(InvoiceStatus.ISSUED)
                .invoiceDate(LocalDate.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        order.setStatus(SalesOrderStatus.INVOICED);
        order.setStockShortageFlagged(false);
        order.setShortageQty(0);
        salesOrderRepository.save(order);

        // Audit Log
        AuditLog audit = AuditLog.builder()
                .entityType("INVOICE")
                .entityId(savedInvoice.getId())
                .action("APPROVE_AND_INVOICE")
                .changedBy(order.getSalesman())
                .details(String.format("Generated invoice %s for order %s. Grand Total: %s",
                        invoiceNumber, order.getOrderNumber(), grandTotal))
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(savedInvoice);
    }

    @Transactional
    public SalesOrderResponse rejectOrder(Long orderId, String reason) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found with id: " + orderId));

        if (order.getStatus() != SalesOrderStatus.SUBMITTED) {
            throw new IllegalArgumentException("Cannot reject order with status: " + order.getStatus());
        }

        order.setStatus(SalesOrderStatus.REJECTED);
        SalesOrder saved = salesOrderRepository.save(order);

        AuditLog audit = AuditLog.builder()
                .entityType("SALES_ORDER")
                .entityId(saved.getId())
                .action("REJECT_ORDER")
                .changedBy(order.getSalesman())
                .details("Order rejected. Reason: " + (reason != null ? reason : "Not specified"))
                .build();
        auditLogRepository.save(audit);

        return SalesOrderResponse.builder()
                .id(saved.getId())
                .orderNumber(saved.getOrderNumber())
                .customerId(saved.getCustomer().getId())
                .customerName(saved.getCustomer().getName())
                .customerState(saved.getCustomer().getStateName())
                .customerGstin(saved.getCustomer().getGstin())
                .salesmanId(saved.getSalesman().getId())
                .salesmanName(saved.getSalesman().getFullName())
                .status(saved.getStatus())
                .stockShortageFlagged(saved.getStockShortageFlagged())
                .shortageQty(saved.getShortageQty())
                .orderDate(saved.getOrderDate())
                .totalEstimatedAmount(BigDecimal.ZERO)
                .lines(List.of())
                .build();
    }

    /**
     * Dispatch & stock deduction:
     * - Validates invoice.status == ISSUED and re-checks live stock.
     * - Inserts negative 'SALE_DISPATCH' rows in stock_ledger.
     * - Updates invoice.status = DISPATCHED, order.status = DISPATCHED.
     */
    @Transactional
    public InvoiceResponse dispatchInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new IllegalArgumentException("Only ISSUED invoices can be dispatched. Current status: " + invoice.getStatus());
        }

        SalesOrder order = invoice.getOrder();

        // Stock check
        for (SalesOrderLine line : order.getLines()) {
            BigDecimal stock = stockLedgerRepository.findBalanceByItemId(line.getItem().getId());
            if (stock.compareTo(BigDecimal.valueOf(line.getQty())) < 0) {
                int shortage = BigDecimal.valueOf(line.getQty()).subtract(stock).intValue();
                throw new InsufficientStockException(String.format(
                        "Cannot dispatch. Insufficient stock for %s. Required: %d, Available: %s, Shortage: %d",
                        line.getItem().getSkuCode(), line.getQty(), stock, shortage
                ));
            }
        }

        // Deduct stock via SALE_DISPATCH
        for (SalesOrderLine line : order.getLines()) {
            StockLedger dispatchEntry = StockLedger.builder()
                    .item(line.getItem())
                    .movementType(MovementType.SALE_DISPATCH)
                    .qtyChange(BigDecimal.valueOf(line.getQty()).negate().setScale(3, RoundingMode.HALF_UP))
                    .unitRate(line.getUnitPrice())
                    .sourceType("INVOICE")
                    .sourceId(invoice.getId())
                    .remarks(String.format("Dispatch for invoice %s, order %s", invoice.getInvoiceNumber(), order.getOrderNumber()))
                    .build();
            stockLedgerRepository.save(dispatchEntry);
        }

        invoice.setStatus(InvoiceStatus.DISPATCHED);
        order.setStatus(SalesOrderStatus.DISPATCHED);

        invoiceRepository.save(invoice);
        salesOrderRepository.save(order);

        AuditLog audit = AuditLog.builder()
                .entityType("INVOICE")
                .entityId(invoice.getId())
                .action("DISPATCH_INVOICE")
                .changedBy(order.getSalesman())
                .details(String.format("Dispatched invoice %s for customer %s",
                        invoice.getInvoiceNumber(), invoice.getCustomer().getName()))
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(invoice);
    }

    /**
     * Invoice cancellation via Credit Note:
     * - Never delete posted invoices.
     * - Sets invoice.status = CANCELLED.
     * - Generates credit note reference (CN/26-27/0001) in audit_logs.
     * - If invoice was already DISPATCHED, inserts reversing positive 'ADJUSTMENT' in stock_ledger.
     */
    @Transactional
    public InvoiceResponse cancelInvoice(Long invoiceId, Long userId, String reason) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalArgumentException("Invoice is already cancelled.");
        }

        boolean wasDispatched = invoice.getStatus() == InvoiceStatus.DISPATCHED;

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);

        // Reverse stock if previously dispatched
        if (wasDispatched && invoice.getOrder() != null) {
            for (SalesOrderLine line : invoice.getOrder().getLines()) {
                StockLedger adjustment = StockLedger.builder()
                        .item(line.getItem())
                        .movementType(MovementType.ADJUSTMENT)
                        .qtyChange(BigDecimal.valueOf(line.getQty()).setScale(3, RoundingMode.HALF_UP))
                        .unitRate(line.getUnitPrice())
                        .sourceType("INVOICE_CANCEL")
                        .sourceId(invoice.getId())
                        .remarks(String.format("Reversal for cancelled invoice %s", invoice.getInvoiceNumber()))
                        .build();
                stockLedgerRepository.save(adjustment);
            }
        }

        long cnCount = auditLogRepository.count() + 1;
        String creditNoteRef = String.format("CN/26-27/%04d", cnCount);

        AuditLog audit = AuditLog.builder()
                .entityType("INVOICE")
                .entityId(invoice.getId())
                .action("CANCEL_VIA_CREDIT_NOTE")
                .details(String.format("Cancelled invoice %s. Credit Note Ref: %s. Reason: %s. Stock restored: %b",
                        invoice.getInvoiceNumber(), creditNoteRef, (reason != null ? reason : "None"), wasDispatched))
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        return mapToResponse(invoice);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        BigDecimal totalPaid = paymentRepository.sumAmountPaidByInvoiceId(invoice.getId());
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        BigDecimal balanceDue = invoice.getGrandTotal().subtract(totalPaid).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .orderId(invoice.getOrder() != null ? invoice.getOrder().getId() : null)
                .orderNumber(invoice.getOrder() != null ? invoice.getOrder().getOrderNumber() : null)
                .customerId(invoice.getCustomer().getId())
                .customerName(invoice.getCustomer().getName())
                .customerState(invoice.getCustomer().getStateName())
                .customerGstin(invoice.getCustomer().getGstin())
                .taxableAmount(invoice.getTaxableAmount())
                .cgstAmount(invoice.getCgstAmount())
                .sgstAmount(invoice.getSgstAmount())
                .igstAmount(invoice.getIgstAmount())
                .grandTotal(invoice.getGrandTotal())
                .totalPaid(totalPaid.setScale(2, RoundingMode.HALF_UP))
                .balanceDue(balanceDue)
                .status(invoice.getStatus())
                .invoiceDate(invoice.getInvoiceDate())
                .build();
    }
}
