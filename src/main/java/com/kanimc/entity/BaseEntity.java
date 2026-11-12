package com.kanimc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "del", length = 3)
    private String del = "   ";

    @Column(name = "crby", length = 65)
    private String crby;

    @Column(name = "modby", length = 65)
    private String modby;

    public String getDel() {
        return del;
    }

    public void setDel(String del) {
        this.del = del;
    }

    public String getCrby() {
        return crby;
    }

    public void setCrby(String crby) {
        this.crby = crby;
    }

    public String getModby() {
        return modby;
    }

    public void setModby(String modby) {
        this.modby = modby;
    }

    @PrePersist
    protected void onCreate() {
        this.crby = "SYS"; // Default for now, can be changed to actual user
        if (this.del == null) this.del = "   ";
    }

    @PreUpdate
    protected void onUpdate() {
        this.modby = "SYS";
    }
}
