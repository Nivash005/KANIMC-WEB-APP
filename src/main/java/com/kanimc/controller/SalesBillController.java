package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.entity.SalesBill;
import com.kanimc.repository.AccountRepository;
import com.kanimc.service.AccountMappingService;
import com.kanimc.service.SalesBillService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales-bills")
@CrossOrigin(origins = "*")
public class SalesBillController {

    private final SalesBillService salesBillService;
    private final com.kanimc.repository.SalesBillRepository salesBillRepository;
    private final AccountRepository accountRepository;
    private final AccountMappingService accountMappingService;

    public SalesBillController(SalesBillService salesBillService, 
                               com.kanimc.repository.SalesBillRepository salesBillRepository,
                               AccountRepository accountRepository, 
                               AccountMappingService accountMappingService) {
        this.salesBillService = salesBillService;
        this.salesBillRepository = salesBillRepository;
        this.accountRepository = accountRepository;
        this.accountMappingService = accountMappingService;
    }

    @GetMapping("/next-no")
    public ResponseEntity<?> getNextBillNo(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        java.util.List<SalesBill> bills = salesBillRepository.findFiltered(targetDate, targetDate, null);
        long max = 0;
        for (SalesBill b : bills) {
            try {
                if (b.getBillNo() != null) {
                    long n = Long.parseLong(b.getBillNo().trim());
                    if (n > max) max = n;
                }
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(java.util.Map.of("nextNo", String.valueOf(max + 1)));
    }

    @PostMapping
    public ResponseEntity<SalesBill> createSalesBill(@RequestBody SalesBill bill) {
        if (bill.getBillNo() != null && bill.getBillDate() != null) {
            java.util.List<SalesBill> existing = salesBillRepository.findFiltered(bill.getBillDate(), bill.getBillDate(), null);
            boolean dup = existing.stream().anyMatch(b -> (bill.getId() == null || !b.getId().equals(bill.getId())) && 
                    b.getBillNo() != null && b.getBillNo().trim().equalsIgnoreCase(bill.getBillNo().trim()));
            if (dup) {
                return ResponseEntity.badRequest().build();
            }
        }
        Long salesAccountId = accountMappingService.getAccountId("SALES", null);
        if (salesAccountId == null) {
            salesAccountId = accountMappingService.getAccountId("BILL", null);
        }
        Account salesAccount = null;
        if (salesAccountId != null) {
            salesAccount = accountRepository.findById(salesAccountId).orElse(null);
        }
        if (salesAccount == null) {
            salesAccount = accountRepository.findByAccountNameIgnoreCase("SALES").orElse(null);
        }
        if (salesAccount == null) {
            salesAccount = accountRepository.findAll().stream()
                    .filter(a -> a.getAccountName() != null && a.getAccountName().toUpperCase().contains("SALES"))
                    .findFirst()
                    .orElse(null);
        }

        if (bill.getBuyerAccount() != null && bill.getBuyerAccount().getId() != null) {
            Account buyer = accountRepository.findById(bill.getBuyerAccount().getId()).orElse(null);
            bill.setBuyerAccount(buyer);
        }

        try {
            SalesBill saved = salesBillService.saveSalesBill(bill, salesAccount);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Iterable<SalesBill>> getAllSalesBills(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        if (startDate != null || endDate != null || accountId != null) {
            return ResponseEntity.ok(salesBillService.getFilteredSalesBills(startDate, endDate, accountId));
        }
        return ResponseEntity.ok(salesBillService.getAllSalesBills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesBill> getSalesBillById(@PathVariable Long id) {
        return salesBillService.getSalesBillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalesBill(@PathVariable Long id) {
        salesBillService.deleteSalesBill(id);
        return ResponseEntity.ok().build();
    }
}
