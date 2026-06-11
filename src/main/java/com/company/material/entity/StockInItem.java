package com.company.material.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "stock_in_item")
public class StockInItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_in_id", nullable = false)
    private StockIn stockIn;

    @Column(nullable = false, length = 30)
    private String materialCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @PrePersist
    public void prePersist() {
        if (this.amount == null && this.quantity != null && this.unitPrice != null) {
            this.amount = this.quantity.multiply(this.unitPrice);
        }
    }
}
