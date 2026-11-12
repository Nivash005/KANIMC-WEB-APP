package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "s_obout")
public class OpeningBalance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tno")
    private Long tno;

    @Column(name = "ob_date", nullable = false)
    private LocalDate obDate;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "atype", length = 10, nullable = false)
    private String atype = "DR"; // DR or CR

    @Column(name = "obdramt", precision = 14, scale = 2)
    private BigDecimal obdramt = BigDecimal.ZERO;

    @Column(name = "obcramt", precision = 14, scale = 2)
    private BigDecimal obcramt = BigDecimal.ZERO;

    public OpeningBalance() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTno() {
        return tno;
    }

    public void setTno(Long tno) {
        this.tno = tno;
    }

    public LocalDate getObDate() {
        return obDate;
    }

    public void setObDate(LocalDate obDate) {
        this.obDate = obDate;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAtype() {
        return atype;
    }

    public void setAtype(String atype) {
        this.atype = atype;
    }

    public BigDecimal getObdramt() {
        return obdramt;
    }

    public void setObdramt(BigDecimal obdramt) {
        this.obdramt = obdramt;
    }

    public BigDecimal getObcramt() {
        return obcramt;
    }

    public void setObcramt(BigDecimal obcramt) {
        this.obcramt = obcramt;
    }
}
