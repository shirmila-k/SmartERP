package com.factoryflow.erp.domain.entity;

import com.factoryflow.erp.domain.enums.SalesOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"customer", "salesman", "lines"})
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salesman_id", nullable = false)
    private User salesman;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrderStatus status = SalesOrderStatus.SUBMITTED;

    @Builder.Default
    @Column(name = "stock_shortage_flagged", nullable = false)
    private Boolean stockShortageFlagged = Boolean.FALSE;

    @Builder.Default
    @Column(name = "shortage_qty", nullable = false)
    private Integer shortageQty = 0;

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesOrderLine> lines = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
}
