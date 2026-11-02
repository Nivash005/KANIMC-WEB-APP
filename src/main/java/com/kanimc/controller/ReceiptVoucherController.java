package com.kanimc.controller;

import com.kanimc.entity.ReceiptVoucher;
import com.kanimc.repository.ReceiptVoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/receipt-vouchers")
@CrossOrigin(origins = "*")
public class ReceiptVoucherController {

    @Autowired
    private ReceiptVoucherRepository receiptVoucherRepository;

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

    @PostMapping
    public ResponseEntity<ReceiptVoucher> createVoucher(@RequestBody ReceiptVoucher voucher) {
        ReceiptVoucher savedVoucher = receiptVoucherRepository.save(voucher); SyncController.bumpVersion();
        return new ResponseEntity<>(savedVoucher, HttpStatus.CREATED);
    }
}
