package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findBySkuCode(String skuCode);
}
