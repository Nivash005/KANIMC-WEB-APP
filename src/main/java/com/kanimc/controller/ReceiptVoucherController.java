package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.entity.ReceiptVoucher;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.ReceiptVoucherRepository;
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
@RequestMapping("/api/receipt-vouchers")
@CrossOrigin(origins = "*")
public class ReceiptVoucherController {

    @Autowired
    private ReceiptVoucherRepository receiptVoucherRepository;

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping
    public List<ReceiptVoucher> getAllVouchers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        if (startDate != null || endDate != null || accountId != null) {
            return receiptVoucherRepository.findFiltered(startDate, endDate, accountId);
        }
        return receiptVoucherRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptVoucher> getVoucherById(@PathVariable Long id) {
        return receiptVoucherRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ReceiptVoucher> createVoucher(@RequestBody ReceiptVoucher voucher) {
        // Resolve account by ID or partyId if needed
        if (voucher.getAccount() != null && voucher.getAccount().getId() != null) {
            Account fullAcc = accountRepository.findById(voucher.getAccount().getId()).orElse(null);
            if (fullAcc != null) {
                voucher.setAccount(fullAcc);
            }
        }
        ReceiptVoucher savedVoucher = receiptVoucherRepository.save(voucher);
        
        // Post double-entry: DR Cash Account, CR Buyer/Merchant Account (reduces buyer balance)
        if (savedVoucher.getAccount() != null && savedVoucher.getAccount().getId() != null &&
            savedVoucher.getAmount() != null && savedVoucher.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account buyer = accountRepository.findById(savedVoucher.getAccount().getId()).orElse(null);
            Account cash = accountRepository.findById(1L).orElseGet(() -> accountRepository.findByPartyId("1"));
            if (buyer != null && cash != null) {
                LocalDate vDate = savedVoucher.getVoucherDate() != null ? savedVoucher.getVoucherDate() : LocalDate.now();
                String docNo = savedVoucher.getVoucherNo() != null ? savedVoucher.getVoucherNo() : String.valueOf(savedVoucher.getId());
                accountingService.postDoubleEntry(
                    vDate,
                    cash,  // DR Cash
                    buyer, // CR Buyer Account (reduces their outstanding balance)
                    savedVoucher.getAmount(),
                    4,     // Doc code 4 = Receipt Voucher
                    docNo,
                    null,
                    savedVoucher.getNarration() != null ? savedVoucher.getNarration() : "Receipt Voucher " + docNo
                );
            }
        }
        
        SyncController.bumpVersion();
        return new ResponseEntity<>(savedVoucher, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReceiptVoucher> updateVoucher(@PathVariable Long id, @RequestBody ReceiptVoucher voucher) {
        ReceiptVoucher existing = receiptVoucherRepository.findById(id).orElse(null);
        if (existing == null && voucher.getVoucherNo() != null) {
            existing = receiptVoucherRepository.findAll().stream()
                    .filter(v -> voucher.getVoucherNo().equals(v.getVoucherNo()))
                    .findFirst().orElse(null);
        }
        if (existing != null) {
            if (voucher.getVoucherDate() != null) existing.setVoucherDate(voucher.getVoucherDate());
            if (voucher.getVoucherNo() != null) existing.setVoucherNo(voucher.getVoucherNo());
            if (voucher.getAmount() != null) existing.setAmount(voucher.getAmount());
            if (voucher.getNarration() != null) existing.setNarration(voucher.getNarration());
            if (voucher.getAccount() != null && voucher.getAccount().getId() != null) {
                Account fullAcc = accountRepository.findById(voucher.getAccount().getId()).orElse(null);
                if (fullAcc != null) {
                    existing.setAccount(fullAcc);
                } else {
                    existing.setAccount(voucher.getAccount());
                }
            }
            ReceiptVoucher saved = receiptVoucherRepository.save(existing);
            SyncController.bumpVersion();
            return ResponseEntity.ok(saved);
        } else {
            return createVoucher(voucher);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        if (receiptVoucherRepository.existsById(id)) {
            receiptVoucherRepository.deleteById(id);
            SyncController.bumpVersion();
        }
        return ResponseEntity.noContent().build();
    }
}
