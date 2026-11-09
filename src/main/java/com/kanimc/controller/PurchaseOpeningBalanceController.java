package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.entity.PurchaseOpeningBalance;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.PurchaseOpeningBalanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-opening-balances")
@CrossOrigin(origins = "*")
public class PurchaseOpeningBalanceController {

    private final PurchaseOpeningBalanceRepository obRepository;
    private final AccountRepository accountRepository;

    public PurchaseOpeningBalanceController(PurchaseOpeningBalanceRepository obRepository,
                                           AccountRepository accountRepository) {
        this.obRepository = obRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<PurchaseOpeningBalance> getAll() {
        return obRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<PurchaseOpeningBalance> create(@RequestBody PurchaseOpeningBalance ob) {
        if (ob.getTraderAccount() != null && ob.getTraderAccount().getId() != null) {
            Account fullTrader = accountRepository.findById(ob.getTraderAccount().getId()).orElse(null);
            if (fullTrader != null) ob.setTraderAccount(fullTrader);
        }
        PurchaseOpeningBalance saved = obRepository.save(ob);
        SyncController.bumpVersion();
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (obRepository.existsById(id)) {
            obRepository.deleteById(id);
            SyncController.bumpVersion();
        }
        return ResponseEntity.noContent().build();
    }
}
