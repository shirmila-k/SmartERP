package com.factoryflow.erp.repository.projection;

import java.math.BigDecimal;

public interface StockBalanceProjection {
    Long getItemId();

    BigDecimal getBalance();
}
