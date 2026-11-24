package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.ProductionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductionRunRepository extends JpaRepository<ProductionRun, Long> {
    Optional<ProductionRun> findByRunCode(String runCode);
}
