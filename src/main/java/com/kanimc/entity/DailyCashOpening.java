package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "f_dbal")
public class DailyCashOpening extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tno")
    private Long tno;

    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    public DailyCashOpening() {}

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

    public LocalDate getBalanceDate() {
        return balanceDate;
    }

    public void setBalanceDate(LocalDate balanceDate) {
        this.balanceDate = balanceDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
