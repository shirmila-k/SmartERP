package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    Optional<SalesOrder> findByOrderNumber(String orderNumber);

    List<SalesOrder> findBySalesmanIdOrderByOrderDateDesc(Long salesmanId);

    List<SalesOrder> findAllByOrderByOrderDateDesc();
}
