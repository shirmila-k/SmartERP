package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrderId(Long orderId);

    List<Invoice> findByCustomerIdOrderByInvoiceDateDesc(Long customerId);

    List<Invoice> findAllByOrderByInvoiceDateDesc();
}
