package com.kanimc.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
public class Purchase extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_no", nullable = false, length = 50)
    private String purchaseNo;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @ManyToOne
    @JoinColumn(name = "trader_account_id")
    private Account traderAccount;

    @ManyToOne
    @JoinColumn(name = "financial_year_id")
    private FinancialYear financialYear;

    @Column(length = 50)
    private String phone;

    @Column(name = "paid_to", length = 150)
    private String paidTo;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal advance = BigDecimal.ZERO;

    @Column(name = "freight_charges", precision = 14, scale = 2)
    private BigDecimal freightCharges = BigDecimal.ZERO;

    @Column(name = "packing_charges", precision = 14, scale = 2)
    private BigDecimal packingCharges = BigDecimal.ZERO;

    @Column(name = "other_charges", precision = 14, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "net_payable", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayable = BigDecimal.ZERO;

    @Column(name = "paid")
    private Boolean paid = false;

    @Column(name = "paid_amount", precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "audit_info", length = 255)
    private String auditInfo;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("purchase")
    private List<PurchaseItem> items = new ArrayList<>();

    public Purchase() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public Account getTraderAccount() { return traderAccount; }
    public void setTraderAccount(Account traderAccount) { this.traderAccount = traderAccount; }

    public FinancialYear getFinancialYear() { return financialYear; }
    public void setFinancialYear(FinancialYear financialYear) { this.financialYear = financialYear; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPaidTo() { return paidTo; }
    public void setPaidTo(String paidTo) { this.paidTo = paidTo; }

    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }

    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }

    public BigDecimal getAdvance() { return advance; }
    public void setAdvance(BigDecimal advance) { this.advance = advance; }

    public BigDecimal getFreightCharges() { return freightCharges; }
    public void setFreightCharges(BigDecimal freightCharges) { this.freightCharges = freightCharges; }

    public BigDecimal getPackingCharges() { return packingCharges; }
    public void setPackingCharges(BigDecimal packingCharges) { this.packingCharges = packingCharges; }

    public BigDecimal getOtherCharges() { return otherCharges; }
    public void setOtherCharges(BigDecimal otherCharges) { this.otherCharges = otherCharges; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getNetPayable() { return netPayable; }
    public void setNetPayable(BigDecimal netPayable) { this.netPayable = netPayable; }

    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public String getAuditInfo() { return auditInfo; }
    public void setAuditInfo(String auditInfo) { this.auditInfo = auditInfo; }

    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) {
        this.items = items;
        if (items != null) {
            for (PurchaseItem it : items) {
                it.setPurchase(this);
            }
        }
    }
}
