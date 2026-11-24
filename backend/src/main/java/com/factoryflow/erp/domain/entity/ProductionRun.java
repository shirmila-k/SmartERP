package com.factoryflow.erp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"finishedItem", "createdBy", "consumptions"})
public class ProductionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "run_code", nullable = false, unique = true, length = 64)
    private String runCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    @Column(name = "batches_planned", nullable = false, precision = 8, scale = 2)
    private BigDecimal batchesPlanned;

    @Column(name = "expected_output_packets", nullable = false)
    private Integer expectedOutputPackets;

    @Column(name = "actual_good_packets", nullable = false)
    private Integer actualGoodPackets;

    @Builder.Default
    @Column(name = "damaged_pouches", nullable = false)
    private Integer damagedPouches = 0;

    @Builder.Default
    @Column(name = "cartons_used", nullable = false)
    private Integer cartonsUsed = 0;

    @Builder.Default
    @Column(name = "labour_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal labourCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "overhead_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal overheadCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "yield_variance_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal yieldVariancePct = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "yield_flagged", nullable = false)
    private Boolean yieldFlagged = Boolean.FALSE;

    @Builder.Default
    @Column(name = "calculated_unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal calculatedUnitCost = BigDecimal.ZERO;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "productionRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionConsumption> consumptions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
