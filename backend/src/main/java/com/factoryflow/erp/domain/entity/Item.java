package com.factoryflow.erp.domain.entity;

import com.factoryflow.erp.domain.enums.ItemType;
import com.factoryflow.erp.domain.enums.UnitOfMeasure;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "sku_code", nullable = false, unique = true, length = 64)
    private String skuCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private UnitOfMeasure unit;

    @Builder.Default
    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "standard_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal standardRate = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Builder.Default
    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "pack_weight_grams")
    private Integer packWeightGrams;

    @Column(name = "packets_per_carton")
    private Integer packetsPerCarton;
}
