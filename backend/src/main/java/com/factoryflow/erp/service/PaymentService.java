package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.Customer;
import com.factoryflow.erp.domain.entity.Invoice;
import com.factoryflow.erp.domain.entity.Payment;
import com.factoryflow.erp.domain.entity.User;
import com.factoryflow.erp.dto.CustomerOutstandingResponse;
import com.factoryflow.erp.dto.PaymentRequest;
import com.factoryflow.erp.dto.PaymentResponse;
import com.factoryflow.erp.exception.ResourceNotFoundException;
import com.factoryflow.erp.repository.CustomerRepository;
import com.factoryflow.erp.repository.InvoiceRepository;
import com.factoryflow.erp.repository.PaymentRepository;
import com.factoryflow.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    /**
     * Records a payment against an invoice:
     * - Prevents overpayment beyond grandTotal - totalPaidSoFar.
     * - Returns payment response with grandTotal, totalPaid, balanceDue.
     */
    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));

        User recordedBy = userRepository.findById(request.getRecordedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getRecordedBy()));

        BigDecimal totalPaidSoFar = paymentRepository.sumAmountPaidByInvoiceId(invoice.getId());
        if (totalPaidSoFar == null) totalPaidSoFar = BigDecimal.ZERO;

        BigDecimal balanceDueBefore = invoice.getGrandTotal().subtract(totalPaidSoFar);
        BigDecimal amountPaid = request.getAmountPaid().setScale(2, RoundingMode.HALF_UP);

        if (amountPaid.compareTo(balanceDueBefore) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Payment of %s exceeds remaining balance due %s for invoice %s",
                    amountPaid, balanceDueBefore, invoice.getInvoiceNumber()
            ));
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amountPaid(amountPaid)
                .paymentMode(request.getPaymentMode())
                .paymentDate(request.getPaymentDate())
                .recordedBy(recordedBy)
                .build();
        Payment saved = paymentRepository.save(payment);

        BigDecimal totalPaidAfter = totalPaidSoFar.add(amountPaid).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceDueAfter = invoice.getGrandTotal().subtract(totalPaidAfter).setScale(2, RoundingMode.HALF_UP);

        return PaymentResponse.builder()
                .id(saved.getId())
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amountPaid(amountPaid)
                .paymentMode(saved.getPaymentMode())
                .paymentDate(saved.getPaymentDate())
                .recordedById(recordedBy.getId())
                .recordedByUsername(recordedBy.getUsername())
                .invoiceGrandTotal(invoice.getGrandTotal())
                .invoiceTotalPaid(totalPaidAfter)
                .invoiceBalanceDue(balanceDueAfter)
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerOutstandingResponse getCustomerOutstanding(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        List<Invoice> invoices = invoiceRepository.findByCustomerIdOrderByInvoiceDateDesc(customerId);

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        List<CustomerOutstandingResponse.InvoiceSummaryDto> invoiceSummaries = new ArrayList<>();

        for (Invoice inv : invoices) {
            BigDecimal paid = paymentRepository.sumAmountPaidByInvoiceId(inv.getId());
            if (paid == null) paid = BigDecimal.ZERO;
            BigDecimal due = inv.getGrandTotal().subtract(paid).max(BigDecimal.ZERO);

            totalInvoiced = totalInvoiced.add(inv.getGrandTotal());
            totalReceived = totalReceived.add(paid);

            invoiceSummaries.add(CustomerOutstandingResponse.InvoiceSummaryDto.builder()
                    .invoiceId(inv.getId())
                    .invoiceNumber(inv.getInvoiceNumber())
                    .grandTotal(inv.getGrandTotal())
                    .totalPaid(paid.setScale(2, RoundingMode.HALF_UP))
                    .balanceDue(due.setScale(2, RoundingMode.HALF_UP))
                    .status(inv.getStatus().name())
                    .build());
        }

        BigDecimal totalOutstanding = totalInvoiced.subtract(totalReceived).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return CustomerOutstandingResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .gstin(customer.getGstin())
                .stateName(customer.getStateName())
                .totalInvoiced(totalInvoiced.setScale(2, RoundingMode.HALF_UP))
                .totalReceived(totalReceived.setScale(2, RoundingMode.HALF_UP))
                .totalOutstanding(totalOutstanding)
                .invoices(invoiceSummaries)
                .build();
    }
}
