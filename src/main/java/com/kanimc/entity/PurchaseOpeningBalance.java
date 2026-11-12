package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_opening_balances")
public class PurchaseOpeningBalance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trader_account_id", nullable = false)
    private Account traderAccount;

    @Column(name = "bill_no", length = 50)
    private String billNo;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "balance", precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "balance_type", length = 10)
    private String balanceType = "CREDIT";

    public PurchaseOpeningBalance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getTraderAccount() { return traderAccount; }
    public void setTraderAccount(Account traderAccount) { this.traderAccount = traderAccount; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getBalanceType() { return balanceType; }
    public void setBalanceType(String balanceType) { this.balanceType = balanceType; }
}
