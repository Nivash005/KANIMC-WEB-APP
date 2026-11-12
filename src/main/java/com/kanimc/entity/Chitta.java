package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chittas")
public class Chitta extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chitta_no", nullable = false, length = 50)
    private String chittaNo;

    @Column(name = "chitta_date", nullable = false)
    private LocalDate chittaDate;

    @ManyToOne
    @JoinColumn(name = "farmer_account_id")
    private Account farmerAccount;

    @ManyToOne
    @JoinColumn(name = "financial_year_id")
    private FinancialYear financialYear;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    // Legacy column retained as the calculated commission amount.
    @Column(name = "commission_deduction", precision = 14, scale = 2)
    private BigDecimal commissionDeduction = BigDecimal.ZERO;

    @Column(name = "commission_rate", precision = 8, scale = 3)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "advance_amount", precision = 14, scale = 2)
    private BigDecimal advanceAmount = BigDecimal.ZERO;

    @Column(name = "freight_amount", precision = 14, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Column(name = "net_payable", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayable;

    @OneToMany(mappedBy = "chitta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChittaItem> items = new java.util.ArrayList<>();

    @Column(name = "paid")
    private Boolean paid = false;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Column(name = "paid_to", length = 100)
    private String paidTo;

    @Column(name = "pay_tno")
    private Long payTno;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;


    public Chitta() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChittaNo() {
        return chittaNo;
    }

    public void setChittaNo(String chittaNo) {
        this.chittaNo = chittaNo;
    }

    public LocalDate getChittaDate() {
        return chittaDate;
    }

    public void setChittaDate(LocalDate chittaDate) {
        this.chittaDate = chittaDate;
    }

    public Account getFarmerAccount() {
        return farmerAccount;
    }

    public void setFarmerAccount(Account farmerAccount) {
        this.farmerAccount = farmerAccount;
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

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getCommissionDeduction() {
        return commissionDeduction;
    }

    public void setCommissionDeduction(BigDecimal commissionDeduction) {
        this.commissionDeduction = commissionDeduction;
    }

    public BigDecimal getNetPayable() {
        return netPayable;
    }

    public void setNetPayable(BigDecimal netPayable) {
        this.netPayable = netPayable;
    }

    public BigDecimal getAdvanceAmount() {
        return advanceAmount;
    }

    public void setAdvanceAmount(BigDecimal advanceAmount) {
        this.advanceAmount = advanceAmount;
    }

    public BigDecimal getFreightAmount() {
        return freightAmount;
    }

    public void setFreightAmount(BigDecimal freightAmount) {
        this.freightAmount = freightAmount;
    }

    public List<ChittaItem> getItems() {
        return items;
    }

    public void setItems(List<ChittaItem> items) {
        this.items = items;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDate payDate) {
        this.payDate = payDate;
    }

    public String getPaidTo() {
        return paidTo;
    }

    public void setPaidTo(String paidTo) {
        this.paidTo = paidTo;
    }

    public Long getPayTno() {
        return payTno;
    }

    public void setPayTno(Long payTno) {
        this.payTno = payTno;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
