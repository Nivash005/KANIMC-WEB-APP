package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_id", unique = true, nullable = false, length = 50)
    private String partyId;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(name = "regional_name", length = 255)
    private String regionalName;

    @ManyToOne
    @JoinColumn(name = "group_code", referencedColumnName = "group_code", nullable = false)
    private AccountGroup accountGroup;

    @Column(name = "phone1", length = 20)
    private String phone1;

    @Column(name = "is_trader")
    private Boolean isTrader = false;

    @Column(name = "is_farmer")
    private Boolean isFarmer = false;

    @Column(name = "opening_balance", precision = 14, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


    public Account() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getRegionalName() {
        return regionalName;
    }

    public void setRegionalName(String regionalName) {
        this.regionalName = regionalName;
    }

    public AccountGroup getAccountGroup() {
        return accountGroup;
    }

    public void setAccountGroup(AccountGroup accountGroup) {
        this.accountGroup = accountGroup;
    }

    public String getPhone1() {
        return phone1;
    }

    public void setPhone1(String phone1) {
        this.phone1 = phone1;
    }

    public Boolean getIsTrader() {
        return isTrader;
    }

    public void setIsTrader(Boolean isTrader) {
        this.isTrader = isTrader;
    }

    public Boolean getIsFarmer() {
        return isFarmer;
    }

    public void setIsFarmer(Boolean isFarmer) {
        this.isFarmer = isFarmer;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
