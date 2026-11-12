package com.kanimc.dto;

import java.math.BigDecimal;

public class OutstandingDTO {
    private Long accountId;
    private String accountName;
    private BigDecimal openingBalance;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal closingBalance;
    private Boolean isFarmer;
    private Boolean isTrader;

    public OutstandingDTO(Long accountId, String accountName, BigDecimal openingBalance, BigDecimal debitAmount, BigDecimal creditAmount, Boolean isFarmer, Boolean isTrader) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.openingBalance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        this.debitAmount = debitAmount != null ? debitAmount : BigDecimal.ZERO;
        this.creditAmount = creditAmount != null ? creditAmount : BigDecimal.ZERO;
        // Balance = Opening + Debit - Credit
        this.closingBalance = this.openingBalance.add(this.debitAmount).subtract(this.creditAmount);
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getDebitAmount() { return debitAmount; }
    public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
    public Boolean getIsFarmer() { return isFarmer; }
    public void setIsFarmer(Boolean isFarmer) { this.isFarmer = isFarmer; }
    public Boolean getIsTrader() { return isTrader; }
    public void setIsTrader(Boolean isTrader) { this.isTrader = isTrader; }
}
