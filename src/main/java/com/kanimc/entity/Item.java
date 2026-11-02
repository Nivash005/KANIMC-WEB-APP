package com.kanimc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icode", unique = true, nullable = false)
    private Integer icode;

    @Column(name = "des", length = 30, nullable = false)
    private String des;

    @Column(name = "dest", length = 30)
    private String dest; // Regional/Tamil name

    @Column(name = "sms_no")
    private Integer smsNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIcode() {
        return icode;
    }

    public void setIcode(Integer icode) {
        this.icode = icode;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public Integer getSmsNo() {
        return smsNo;
    }

    public void setSmsNo(Integer smsNo) {
        this.smsNo = smsNo;
    }
}
