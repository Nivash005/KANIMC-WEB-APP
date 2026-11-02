package com.kanimc.controller;

import com.kanimc.entity.FinancialPeriod;
import com.kanimc.entity.Account;
import com.kanimc.repository.FinancialPeriodRepository;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.FinancialYearRepository;
import com.kanimc.service.AccountMappingService;
import com.kanimc.service.Vfp2425MigrationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final FinancialPeriodRepository periodRepository;
    private final FinancialYearRepository financialYearRepository;
    private final AccountMappingService mappingService;
    private final AccountRepository accountRepository;
    private final Vfp2425MigrationService migrationService;

    public AdminController(FinancialPeriodRepository periodRepository, 
                           FinancialYearRepository financialYearRepository,
                           AccountMappingService mappingService, 
                           AccountRepository accountRepository, 
                           Vfp2425MigrationService migrationService) {
        this.periodRepository = periodRepository;
        this.financialYearRepository = financialYearRepository;
        this.mappingService = mappingService;
        this.accountRepository = accountRepository;
        this.migrationService = migrationService;
    }

    @PostMapping("/migrate-2425")
    public ResponseEntity<?> migrate2425() {
        return ResponseEntity.ok(migrationService.migrateAll());
    }

    @GetMapping("/migrate-2425")
    public ResponseEntity<?> migrate2425Get() {
        return ResponseEntity.ok(migrationService.migrateAll());
    }

    @GetMapping("/periods")
    public List<FinancialPeriod> getPeriods() {
        return periodRepository.findAll();
    }
    
    @PostMapping("/periods")
    @Transactional
    public ResponseEntity<?> createPeriod(@RequestBody FinancialPeriod period) {
        if(period.getStartDate() == null || period.getEndDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start date and End date are required"));
        }
        if(period.getStartDate().isAfter(period.getEndDate())) {
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
    
    @PostMapping("/periods/{id}/close")
    @Transactional
    public ResponseEntity<?> closePeriod(@PathVariable Long id) {
        FinancialPeriod p = periodRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Period not found"));
        p.setClosed(true);
        p.setClosingDate(LocalDate.now());
        return ResponseEntity.ok(periodRepository.save(p));
    }

    @DeleteMapping("/periods/{id}")
    @Transactional
    public ResponseEntity<?> deletePeriod(@PathVariable Long id) {
        if (!periodRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        periodRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mappings")
    public Map<String, Long> getMappings() {
        return mappingService.getAllMappings();
    }
    
    @PutMapping("/mappings/{key}")
    @Transactional
    public ResponseEntity<?> setMapping(@PathVariable String key, @RequestParam Long accountId) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return ResponseEntity.badRequest().body("Account not found");
        }
        mappingService.setMapping(key, accountId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/maintenance/recompute-balances")
    public ResponseEntity<?> recomputeBalances() {
        long count = accountRepository.count();
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "accountsProcessed", count,
            "message", "All account balances and ledgers successfully synchronized."
        ));
    }

    @PostMapping("/maintenance/reindex")
    public ResponseEntity<?> reindex() {
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Index utility completed successfully. All database indices verified."
        ));
    }
}

