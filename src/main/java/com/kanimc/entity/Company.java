package com.kanimc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "c_list")
public class Company extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comname", length = 100)
    private String comname;

    @Column(name = "comsn", length = 30)
    private String comsn;

    @Column(name = "comadd1", length = 100)
    private String comadd1;

    @Column(name = "comadd2", length = 100)
    private String comadd2;

    @Column(name = "comadd3", length = 100)
    private String comadd3;

    @Column(name = "comadd4", length = 100)
    private String comadd4;

    @Column(name = "comadd5", length = 100)
    private String comadd5;

    @Column(name = "complace", length = 50)
    private String complace;

    @Column(name = "comphone1", length = 30)
    private String comphone1;

    @Column(name = "comphone2", length = 30)
    private String comphone2;

    @Column(name = "comtin", length = 30)
    private String comtin;

    @Column(name = "comcst", length = 30)
    private String comcst;

    @Column(name = "ciac", length = 30)
    private String ciac;

    @Column(name = "mtopg")
    private Integer mtopg;

    @Column(name = "mbotg")
    private Integer mbotg;

    @Column(name = "mbotmes", length = 200)
    private String mbotmes;

    @Column(name = "btitle", length = 100)
    private String btitle;

    @Column(name = "drcr", length = 10)
    private String drcr;

    @Column(name = "mvat")
    private Boolean mvat;

    @Column(name = "mvatdate")
    private LocalDate mvatdate;

    @Column(name = "vati", length = 30)
    private String vati;

    public Company() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComname() {
        return comname;
    }

    public void setComname(String comname) {
        this.comname = comname;
    }

    public String getComsn() {
        return comsn;
    }

    public void setComsn(String comsn) {
        this.comsn = comsn;
    }

    public String getComadd1() {
        return comadd1;
    }

    public void setComadd1(String comadd1) {
        this.comadd1 = comadd1;
    }

    public String getComadd2() {
        return comadd2;
    }

    public void setComadd2(String comadd2) {
        this.comadd2 = comadd2;
    }

    public String getComadd3() {
        return comadd3;
    }

    public void setComadd3(String comadd3) {
        this.comadd3 = comadd3;
    }

    public String getComadd4() {
        return comadd4;
    }

    public void setComadd4(String comadd4) {
        this.comadd4 = comadd4;
    }

    public String getComadd5() {
        return comadd5;
    }

    public void setComadd5(String comadd5) {
        this.comadd5 = comadd5;
    }

    public String getComplace() {
        return complace;
    }

    public void setComplace(String complace) {
        this.complace = complace;
    }

    public String getComphone1() {
        return comphone1;
    }

    public void setComphone1(String compphone1) {
        this.comphone1 = compphone1;
    }

    public String getComphone2() {
        return comphone2;
    }

    public void setComphone2(String comphone2) {
        this.comphone2 = comphone2;
    }

    public String getComtin() {
        return comtin;
    }

    public void setComtin(String comtin) {
        this.comtin = comtin;
    }

    public String getComcst() {
        return comcst;
    }

    public void setComcst(String comcst) {
        this.comcst = comcst;
    }

    public String getCiac() {
        return ciac;
    }

    public void setCiac(String ciac) {
        this.ciac = ciac;
    }

    public Integer getMtopg() {
        return mtopg;
    }

    public void setMtopg(Integer mtopg) {
        this.mtopg = mtopg;
    }

    public Integer getMbotg() {
        return mbotg;
    }

    public void setMbotg(Integer mbotg) {
        this.mbotg = mbotg;
    }

    public String getMbotmes() {
        return mbotmes;
    }

    public void setMbotmes(String mbotmes) {
        this.mbotmes = mbotmes;
    }

    public String getBtitle() {
        return btitle;
    }

    public void setBtitle(String btitle) {
        this.btitle = btitle;
    }

    public String getDrcr() {
        return drcr;
    }

    public void setDrcr(String drcr) {
        this.drcr = drcr;
    }

    public Boolean getMvat() {
        return mvat;
    }

    public void setMvat(Boolean mvat) {
        this.mvat = mvat;
    }

    public LocalDate getMvatdate() {
        return mvatdate;
    }

    public void setMvatdate(LocalDate mvatdate) {
        this.mvatdate = mvatdate;
    }

    public String getVati() {
        return vati;
    }

    public void setVati(String vati) {
        this.vati = vati;
    }
}
