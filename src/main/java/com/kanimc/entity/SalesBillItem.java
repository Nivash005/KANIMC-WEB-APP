package com.kanimc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sales_bill_items")
public class SalesBillItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sales_bill_id", nullable = false)
    @JsonIgnore
    private SalesBill salesBill;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private Integer bags = 0;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    public SalesBillItem() {}

    @PrePersist
    @PreUpdate
    public void ensureDefaults() {
        if (this.bags == null) this.bags = 0;
        if (this.quantity == null) this.quantity = BigDecimal.ZERO;
        if (this.rate == null) this.rate = BigDecimal.ZERO;
        if (this.amount == null) this.amount = this.quantity.multiply(this.rate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SalesBill getSalesBill() {
        return salesBill;
    }

    public void setSalesBill(SalesBill salesBill) {
        this.salesBill = salesBill;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Integer getBags() {
        return bags != null ? bags : 0;
    }

    public void setBags(Integer bags) {
        this.bags = bags != null ? bags : 0;
    }

    public BigDecimal getQuantity() {
        return quantity != null ? quantity : BigDecimal.ZERO;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
    }

    public BigDecimal getRate() {
        return rate != null ? rate : BigDecimal.ZERO;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate != null ? rate : BigDecimal.ZERO;
    }

    public BigDecimal getAmount() {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }
}
