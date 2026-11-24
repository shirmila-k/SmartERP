package com.factoryflow.erp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bom_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"bom", "componentItem"})
public class BomLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private BomHeader bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_item_id", nullable = false)
    private Item componentItem;

    @Column(name = "standard_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal standardQty;

    @Builder.Default
    @Column(name = "is_packaging", nullable = false)
    private Boolean isPackaging = Boolean.FALSE;
}
