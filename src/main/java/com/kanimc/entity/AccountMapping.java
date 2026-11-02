package com.kanimc.entity;
import jakarta.persistence.*;
@Entity
public class AccountMapping {
    @Id private String mappingType;
    private Long accountId;
    public AccountMapping() {}
    public AccountMapping(String mappingType, Long accountId) { this.mappingType = mappingType; this.accountId = accountId; }
    public String getMappingType() { return mappingType; }
    public void setMappingType(String mappingType) { this.mappingType = mappingType; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
