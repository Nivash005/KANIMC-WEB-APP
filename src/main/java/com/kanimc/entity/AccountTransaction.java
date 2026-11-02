package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "account_transactions")
public class AccountTransaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "doc_code", nullable = false)
    private Integer docCode; // 1=Receipt, 2=Payment, 4=Chitta, 6=Sales, O=Opening

    @Column(name = "doc_no", length = 50)
    private String docNo;

    @Column(name = "debit_amount", precision = 14, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", precision = 14, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String narration;

    @ManyToOne
    @JoinColumn(name = "financial_year_id")
    private FinancialYear financialYear;


    public AccountTransaction() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Integer getDocCode() {
        return docCode;
    }

    public void setDocCode(Integer docCode) {
        this.docCode = docCode;
    }

    public String getDocNo() {
        return docNo;
    }

    public void setDocNo(String docNo) {
        this.docNo = docNo;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public FinancialYear getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(FinancialYear financialYear) {
        this.financialYear = financialYear;
    }
}
