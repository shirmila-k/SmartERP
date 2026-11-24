package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.BomHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BomHeaderRepository extends JpaRepository<BomHeader, Long> {
    Optional<BomHeader> findByFinishedItemId(Long finishedItemId);
}
