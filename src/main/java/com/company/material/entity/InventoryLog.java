package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_log")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String materialCode;

    @Column(nullable = false, length = 30)
    private String warehouseCode;

    @Column(nullable = false, length = 20)
    private String changeType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal changeQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal beforeQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal afterQuantity;

    @Column(length = 30)
    private String relatedBillNo;

    @Column(length = 50)
    private String operator;

    @Column(nullable = false)
    private LocalDateTime operateTime;

    @PrePersist
    public void prePersist() {
        if (this.operateTime == null) this.operateTime = LocalDateTime.now();
    }
}
