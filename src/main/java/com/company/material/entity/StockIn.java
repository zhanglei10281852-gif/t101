package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "stock_in")
public class StockIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String stockInNo;

    @Column(nullable = false, length = 20)
    private String stockInType;

    @Column(length = 30)
    private String supplierCode;

    @Column(nullable = false, length = 30)
    private String warehouseCode;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 50)
    private String handler;

    private LocalDate stockInDate;

    @Column(length = 200)
    private String remark;

    @Column(nullable = false, length = 20)
    private String status;

    private LocalDateTime auditedAt;

    private String auditor;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "stockIn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StockInItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "待审核";
        if (this.stockInDate == null) this.stockInDate = LocalDate.now();
        if (this.totalAmount == null) this.totalAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
