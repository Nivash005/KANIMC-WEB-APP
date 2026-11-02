package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "sales_bills")
public class SalesBill extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_no", nullable = false, length = 50)
    private String billNo;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @ManyToOne
    @JoinColumn(name = "buyer_account_id")
    private Account buyerAccount;

    @ManyToOne
    @JoinColumn(name = "financial_year_id")
    private FinancialYear financialYear;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_charges", precision = 14, scale = 2)
    private BigDecimal commissionCharges = BigDecimal.ZERO;

    @Column(name = "packing_charges", precision = 14, scale = 2)
    private BigDecimal packingCharges = BigDecimal.ZERO;

    @Column(name = "freight_charges", precision = 14, scale = 2)
    private BigDecimal freightCharges = BigDecimal.ZERO;

    @Column(name = "other_charges", precision = 14, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "net_receivable", nullable = false, precision = 14, scale = 2)
    private BigDecimal netReceivable;

    @OneToMany(mappedBy = "salesBill", cascade = CascadeType.ALL)
    private List<SalesBillItem> items;


    public SalesBill() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }

    public Account getBuyerAccount() {
        return buyerAccount;
    }

    public void setBuyerAccount(Account buyerAccount) {
        this.buyerAccount = buyerAccount;
    }

    public FinancialYear getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(FinancialYear financialYear) {
        this.financialYear = financialYear;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getCommissionCharges() {
        return commissionCharges;
    }

    public void setCommissionCharges(BigDecimal commissionCharges) {
        this.commissionCharges = commissionCharges;
    }

    public BigDecimal getPackingCharges() {
        return packingCharges;
    }

    public void setPackingCharges(BigDecimal packingCharges) {
        this.packingCharges = packingCharges;
    }

    public BigDecimal getFreightCharges() {
        return freightCharges;
    }

    public void setFreightCharges(BigDecimal freightCharges) {
        this.freightCharges = freightCharges;
    }

    public BigDecimal getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(BigDecimal otherCharges) {
        this.otherCharges = otherCharges;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getNetReceivable() {
        return netReceivable;
    }

    public void setNetReceivable(BigDecimal netReceivable) {
        this.netReceivable = netReceivable;
    }

    public List<SalesBillItem> getItems() {
        return items;
    }

    public void setItems(List<SalesBillItem> items) {
        this.items = items;
    }
}
