package com.kanimc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "sini")
public class SoftwareSetting extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zbpfile", length = 100)
    private String zbpfile;

    @Column(name = "zslreg", length = 100)
    private String zslreg;

    @Column(name = "zbillpfile", length = 100)
    private String zbillpfile;

    @Column(name = "ziscom")
    private Boolean ziscom;

    @Column(name = "zchittano")
    private Long zchittano;

    @Column(name = "zbillno")
    private Long zbillno;

    @Column(name = "mcustagcode")
    private Integer mcustagcode;

    @Column(name = "mfaragcode")
    private Integer mfaragcode;

    @Column(name = "tyldate")
    private LocalDate tyldate;

    public SoftwareSetting() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getZbpfile() {
        return zbpfile;
    }

    public void setZbpfile(String zbpfile) {
        this.zbpfile = zbpfile;
    }

    public String getZslreg() {
        return zslreg;
    }

    public void setZslreg(String zslreg) {
        this.zslreg = zslreg;
    }

    public String getZbillpfile() {
        return zbillpfile;
    }

    public void setZbillpfile(String zbillpfile) {
        this.zbillpfile = zbillpfile;
    }

    public Boolean getZiscom() {
        return ziscom;
    }

    public void setZiscom(Boolean ziscom) {
        this.ziscom = ziscom;
    }

    public Long getZchittano() {
        return zchittano;
    }

    public void setZchittano(Long zchittano) {
        this.zchittano = zchittano;
    }

    public Long getZbillno() {
        return zbillno;
    }

    public void setZbillno(Long zbillno) {
        this.zbillno = zbillno;
    }

    public Integer getMcustagcode() {
        return mcustagcode;
    }

    public void setMcustagcode(Integer mcustagcode) {
        this.mcustagcode = mcustagcode;
    }

    public Integer getMfaragcode() {
        return mfaragcode;
    }

    public void setMfaragcode(Integer mfaragcode) {
        this.mfaragcode = mfaragcode;
    }

    public LocalDate getTyldate() {
        return tyldate;
    }

    public void setTyldate(LocalDate tyldate) {
        this.tyldate = tyldate;
    }
}
