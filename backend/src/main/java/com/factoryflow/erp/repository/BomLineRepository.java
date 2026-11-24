package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.BomLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BomLineRepository extends JpaRepository<BomLine, Long> {
    List<BomLine> findByBomId(Long bomId);
}
