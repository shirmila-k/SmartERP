package com.factoryflow.erp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "production_consumptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"productionRun", "item"})
public class ProductionConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_run_id", nullable = false)
    private ProductionRun productionRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "planned_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal plannedQty;

    @Column(name = "actual_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal actualQty;

    @Builder.Default
    @Column(name = "variance_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal variancePct = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "variance_flagged", nullable = false)
    private Boolean varianceFlagged = Boolean.FALSE;

    @Builder.Default
    @Column(name = "unit_cost_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCostApplied = BigDecimal.ZERO;
}
