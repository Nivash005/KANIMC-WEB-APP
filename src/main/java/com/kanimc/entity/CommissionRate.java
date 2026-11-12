package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "s_rate")
public class CommissionRate extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rateid")
    private Long rateid;

    @Column(name = "wefdate", nullable = false)
    private LocalDate wefdate;

    @Column(name = "comp", precision = 8, scale = 2, nullable = false)
    private BigDecimal comp;

    public CommissionRate() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRateid() {
        return rateid;
    }

    public void setRateid(Long rateid) {
        this.rateid = rateid;
    }

    public LocalDate getWefdate() {
        return wefdate;
    }

    public void setWefdate(LocalDate wefdate) {
        this.wefdate = wefdate;
    }

    public BigDecimal getComp() {
        return comp;
    }

    public void setComp(BigDecimal comp) {
        this.comp = comp;
    }
}
