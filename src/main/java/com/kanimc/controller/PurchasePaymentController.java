package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.entity.Purchase;
import com.kanimc.entity.PurchasePayment;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.PurchasePaymentRepository;
import com.kanimc.repository.PurchaseRepository;
import com.kanimc.service.AccountingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-payments")
@CrossOrigin(origins = "*")
public class PurchasePaymentController {

    private final PurchasePaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;

    public PurchasePaymentController(PurchasePaymentRepository paymentRepository,
                                     PurchaseRepository purchaseRepository,
                                     AccountRepository accountRepository,
                                     AccountingService accountingService) {
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
    }

    @GetMapping
    public List<PurchasePayment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<PurchasePayment> createPayment(@RequestBody PurchasePayment payment) {
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }

        if (payment.getTraderAccount() != null && payment.getTraderAccount().getId() != null) {
            Account fullTrader = accountRepository.findById(payment.getTraderAccount().getId()).orElse(null);
            if (fullTrader != null) payment.setTraderAccount(fullTrader);
        }

        if (payment.getPurchase() != null && payment.getPurchase().getId() != null) {
            Purchase p = purchaseRepository.findById(payment.getPurchase().getId()).orElse(null);
            if (p != null) {
                p.setPaid(true);
                p.setPaidAmount(payment.getAmount());
                p.setPayDate(payment.getPaymentDate());
                purchaseRepository.save(p);
                payment.setPurchase(p);
            }
        }

        PurchasePayment saved = paymentRepository.save(payment);

        // Double Entry: DR Trader Account, CR Cash Account
        Account traderAcc = saved.getTraderAccount();
        Account cashAcc = accountRepository.findAll().stream()
                .filter(a -> a.getAccountName() != null && "CASH".equalsIgnoreCase(a.getAccountName().trim()))
                .findFirst().orElseGet(() -> accountRepository.findById(1L).orElse(null));

        BigDecimal amt = saved.getAmount();
        if (traderAcc != null && cashAcc != null && amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postDoubleEntry(
                saved.getPaymentDate(),
                traderAcc, // DR Trader Account (reduces trader payable)
                cashAcc,   // CR Cash Account
                amt,
                6,         // Doc Code 6 = Purchase Payment
                saved.getPaymentNo() != null ? saved.getPaymentNo() : String.valueOf(saved.getId()),
                null,
                "Purchase Payment"
            );
        }

        SyncController.bumpVersion();
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        if (paymentRepository.existsById(id)) {
            paymentRepository.deleteById(id);
            SyncController.bumpVersion();
        }
        return ResponseEntity.noContent().build();
    }
}
