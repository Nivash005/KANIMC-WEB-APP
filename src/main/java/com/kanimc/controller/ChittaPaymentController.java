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
@RequestMapping("/api/chitta-payments")
@CrossOrigin(origins = "*")
public class ChittaPaymentController {

    private final AccountingService accountingService;
    private final AccountRepository accountRepository;

    public ChittaPaymentController(AccountingService accountingService, AccountRepository accountRepository) {
        this.accountingService = accountingService;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<String> makeChittaPayment(@RequestParam Long farmerId, @RequestParam BigDecimal amount) {
        Account cashAccount = accountRepository.findById(1L).orElse(null);
        Account farmerAccount = accountRepository.findById(farmerId).orElse(null);

        if (cashAccount == null || farmerAccount == null) return ResponseEntity.badRequest().body("Accounts not found");

        try {
            accountingService.postDoubleEntry(LocalDate.now(), farmerAccount, cashAccount, amount, 3, "CP-" + System.currentTimeMillis(), null, "Chitta Payment");
            return ResponseEntity.ok("Payment Successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}