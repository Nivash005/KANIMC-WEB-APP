package com.kanimc.controller;

import com.kanimc.entity.FinancialPeriod;
import com.kanimc.repository.FinancialPeriodRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/periods")
@CrossOrigin(origins = "*")
public class PeriodController {

    private final FinancialPeriodRepository periodRepository;
    private final com.kanimc.repository.FinancialYearRepository financialYearRepository;

    public PeriodController(FinancialPeriodRepository periodRepository,
                            com.kanimc.repository.FinancialYearRepository financialYearRepository) {
        this.periodRepository = periodRepository;
        this.financialYearRepository = financialYearRepository;
    }

    @GetMapping
    public List<FinancialPeriod> getAllPeriods() {
        return periodRepository.findAll(Sort.by(Sort.Direction.ASC, "startDate"));
    }

    @PostMapping
    public ResponseEntity<?> createPeriod(@RequestBody FinancialPeriod period) {
        if (period.getStartDate() == null || period.getEndDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start date and End date are required"));
        }
        if (period.getStartDate().isAfter(period.getEndDate())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start date cannot be after end date"));
        }
        
        List<FinancialPeriod> existing = periodRepository.findAll();
        for (FinancialPeriod p : existing) {
            if (period.getStartDate().isEqual(p.getStartDate()) && period.getEndDate().isEqual(p.getEndDate())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Duplicate financial period: " + period.getPeriodName() + " already exists."));
            }
        }
        
        period.setClosed(false);
        FinancialPeriod savedPeriod = periodRepository.save(period);

        // Sync FinancialYear
        String yearCode = period.getStartDate().getYear() + "-" + period.getEndDate().getYear();
        financialYearRepository.findByYearCode(yearCode).orElseGet(() -> {
            com.kanimc.entity.FinancialYear fy = new com.kanimc.entity.FinancialYear();
            fy.setYearCode(yearCode);
            fy.setStartDate(period.getStartDate());
            fy.setEndDate(period.getEndDate());
            fy.setIsActive(true);
            return financialYearRepository.save(fy);
        });

        return ResponseEntity.ok(savedPeriod);
    }
}
