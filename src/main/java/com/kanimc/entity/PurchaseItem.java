package com.kanimc.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_items")
public class PurchaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    @JsonIgnore
    private Purchase purchase;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "item_description", length = 150)
    private String itemDescription;

    @Column(length = 150)
    private String particulars;

    @Column(name = "bags", precision = 10, scale = 2)
    private BigDecimal bags = BigDecimal.ZERO;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "rate", precision = 10, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    public PurchaseItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase purchase) { this.purchase = purchase; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public String getParticulars() { return particulars; }
    public void setParticulars(String particulars) { this.particulars = particulars; }

    public BigDecimal getBags() { return bags; }
    public void setBags(BigDecimal bags) { this.bags = bags; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
