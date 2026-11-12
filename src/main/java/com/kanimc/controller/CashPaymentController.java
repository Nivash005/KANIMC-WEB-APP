package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.repository.AccountRepository;
import com.kanimc.service.AccountingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/cash-payments")
@CrossOrigin(origins = "*")
public class CashPaymentController {

    private final AccountingService accountingService;
    private final AccountRepository accountRepository;

    public CashPaymentController(AccountingService accountingService, AccountRepository accountRepository) {
        this.accountingService = accountingService;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<String> makeCashPayment(@RequestParam Long accountId, @RequestParam BigDecimal amount, @RequestParam String particulars) {
        Account cashAccount = accountRepository.findById(1L).orElse(null);
        Account targetAccount = accountRepository.findById(accountId).orElse(null);

        if (cashAccount == null || targetAccount == null) return ResponseEntity.badRequest().body("Accounts not found");

        try {
            accountingService.postDoubleEntry(LocalDate.now(), targetAccount, cashAccount, amount, 2, "PAY-" + System.currentTimeMillis(), null, particulars);
            return ResponseEntity.ok("Payment Successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}