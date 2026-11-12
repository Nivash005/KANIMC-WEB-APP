package com.kanimc.service;

import com.kanimc.entity.CommissionRate;
import com.kanimc.repository.CommissionRateRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CommissionRateService {
    private final CommissionRateRepository repository;

    public CommissionRateService(CommissionRateRepository repository) {
        this.repository = repository;
    }

    public List<CommissionRate> getAllRates() {
        return repository.findAll();
    }

    public Optional<CommissionRate> getRateById(Long id) {
        return repository.findById(id);
    }

    public CommissionRate saveRate(CommissionRate rate) {
        return repository.save(rate);
    }

    public void deleteRate(Long id) {
        repository.deleteById(id);
    }

    public BigDecimal getEffectiveRate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        return repository.findFirstByWefdateLessThanEqualOrderByWefdateDesc(date)
                .map(CommissionRate::getComp)
                .orElse(BigDecimal.valueOf(10.0)); // Default 10%
    }
}
