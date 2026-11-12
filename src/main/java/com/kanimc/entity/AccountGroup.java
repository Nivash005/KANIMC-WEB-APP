package com.kanimc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "account_groups")
public class AccountGroup extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", unique = true, nullable = false)
    private Integer groupCode;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @ManyToOne
    @JoinColumn(name = "parent_group_id")
    private AccountGroup parentGroup;

    @Column(name = "agtype", length = 50)
    private String agtype;

    @Column(name = "area", length = 50)
    private String area;

    @Column(name = "sltax", precision = 8, scale = 2)
    private java.math.BigDecimal sltax;

    @Column(name = "agout", precision = 14, scale = 2)
    private java.math.BigDecimal agout;

    public AccountGroup() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(Integer groupCode) {
        this.groupCode = groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public AccountGroup getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(AccountGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    public String getAgtype() {
        return agtype;
    }

    public void setAgtype(String agtype) {
        this.agtype = agtype;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public java.math.BigDecimal getSltax() {
        return sltax;
    }

    public void setSltax(java.math.BigDecimal sltax) {
        this.sltax = sltax;
    }

    public java.math.BigDecimal getAgout() {
        return agout;
    }

    public void setAgout(java.math.BigDecimal agout) {
        this.agout = agout;
    }
}
