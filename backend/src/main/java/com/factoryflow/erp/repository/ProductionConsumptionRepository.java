package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.ProductionConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionConsumptionRepository extends JpaRepository<ProductionConsumption, Long> {
    List<ProductionConsumption> findByProductionRunId(Long productionRunId);
}
