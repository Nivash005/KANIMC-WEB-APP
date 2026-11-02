package com.kanimc.controller;

import com.kanimc.entity.CashVoucher;
import com.kanimc.repository.CashVoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cash-vouchers")
@CrossOrigin(origins = "*")
public class CashVoucherController {

    @Autowired
    private CashVoucherRepository cashVoucherRepository;

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
        CashVoucher savedVoucher = cashVoucherRepository.save(voucher); SyncController.bumpVersion();
        return new ResponseEntity<>(savedVoucher, HttpStatus.CREATED);
    }
}
