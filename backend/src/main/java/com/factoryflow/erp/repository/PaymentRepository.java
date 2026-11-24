package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceId(Long invoiceId);

    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            WHERE p.invoice.id = :invoiceId
            """)
    BigDecimal sumAmountPaidByInvoiceId(@Param("invoiceId") Long invoiceId);
}
