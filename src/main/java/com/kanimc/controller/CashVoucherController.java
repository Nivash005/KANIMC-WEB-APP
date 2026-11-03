package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.entity.CashVoucher;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.CashVoucherRepository;
import com.kanimc.service.AccountingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cash-vouchers")
@CrossOrigin(origins = "*")
public class CashVoucherController {

    @Autowired
    private CashVoucherRepository cashVoucherRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountingService accountingService;

    @GetMapping
    public List<CashVoucher> getAllVouchers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        if (startDate != null || endDate != null || accountId != null) {
            return cashVoucherRepository.findFiltered(startDate, endDate, accountId);
        }
        return cashVoucherRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<CashVoucher> createVoucher(@RequestBody CashVoucher voucher) {
        CashVoucher savedVoucher = cashVoucherRepository.save(voucher);
        
        // Post double-entry: DR Target Account, CR Cash Account (reduces target balance / records cash payment)
        if (savedVoucher.getAccount() != null && savedVoucher.getAccount().getId() != null &&
            savedVoucher.getAmount() != null && savedVoucher.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account target = accountRepository.findById(savedVoucher.getAccount().getId()).orElse(null);
            Account cash = accountRepository.findById(1L).orElseGet(() -> accountRepository.findByPartyId("1"));
            if (target != null && cash != null) {
                LocalDate vDate = savedVoucher.getVoucherDate() != null ? savedVoucher.getVoucherDate() : LocalDate.now();
                String docNo = savedVoucher.getVoucherNo() != null ? savedVoucher.getVoucherNo() : String.valueOf(savedVoucher.getId());
                accountingService.postDoubleEntry(
                    vDate,
                    target, // DR Target Account
                    cash,   // CR Cash Account
                    savedVoucher.getAmount(),
                    2,      // Doc code 2 = Cash Payment Voucher
                    docNo,
                    null,
                    savedVoucher.getNarration() != null ? savedVoucher.getNarration() : "Cash Payment " + docNo
                );
            }
        }
        
        SyncController.bumpVersion();
        return new ResponseEntity<>(savedVoucher, HttpStatus.CREATED);
    }
}
