package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.SalesOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {
    List<SalesOrderLine> findByOrderId(Long orderId);
}
