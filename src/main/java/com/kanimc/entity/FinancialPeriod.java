package com.kanimc.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity
public class FinancialPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate closingDate;
    private boolean isClosed;
    
    public Long getId() { return id; }
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDate closingDate) { this.closingDate = closingDate; }
    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }
}
