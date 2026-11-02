package com.kanimc.dto;

import java.math.BigDecimal;

public class ItemSummaryDTO {
    private Long itemId;
    private String itemName;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;

    public ItemSummaryDTO(Long itemId, String itemName, BigDecimal totalQuantity, BigDecimal totalAmount) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
