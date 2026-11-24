package com.factoryflow.erp.repository;

import com.factoryflow.erp.domain.entity.StockLedger;
import com.factoryflow.erp.repository.projection.StockBalanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    @Query("""
            SELECT s.item.id AS itemId,
                   COALESCE(SUM(s.qtyChange), 0) AS balance
            FROM StockLedger s
            GROUP BY s.item.id
            """)
    List<StockBalanceProjection> findStockBalancesGroupedByItem();

    @Query("""
            SELECT COALESCE(SUM(s.qtyChange), 0)
            FROM StockLedger s
            WHERE s.item.id = :itemId
            """)
    BigDecimal findBalanceByItemId(@Param("itemId") Long itemId);
}
