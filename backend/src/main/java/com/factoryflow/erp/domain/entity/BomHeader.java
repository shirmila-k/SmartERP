package com.factoryflow.erp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bom_headers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"finishedItem", "lines"})
public class BomHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false, unique = true)
    private Item finishedItem;

    @Column(name = "recipe_group", nullable = false, length = 100)
    private String recipeGroup;

    @Column(name = "expected_yield_packets", nullable = false)
    private Integer expectedYieldPackets;

    @Builder.Default
    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomLine> lines = new ArrayList<>();
}
