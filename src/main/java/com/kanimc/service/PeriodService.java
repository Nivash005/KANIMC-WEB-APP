package com.kanimc.service;
import com.kanimc.repository.FinancialPeriodRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
@Service
public class PeriodService {
    private final FinancialPeriodRepository repo;
    public PeriodService(FinancialPeriodRepository repo) { this.repo = repo; }
    public void validateDate(LocalDate date) {
        repo.findAll().stream().filter(p -> p.isClosed() && (date.isBefore(p.getClosingDate()) || date.isEqual(p.getClosingDate()))).findFirst().ifPresent(p -> {
            throw new RuntimeException("Accounting period is closed for date: " + date);
        });
    }
}
