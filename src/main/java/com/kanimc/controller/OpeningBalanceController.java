package com.kanimc.controller;

import com.kanimc.entity.OpeningBalance;
import com.kanimc.service.OpeningBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/opening-balances")
public class OpeningBalanceController {
    private final OpeningBalanceService service;

    public OpeningBalanceController(OpeningBalanceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<OpeningBalance>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<OpeningBalance>> getByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(service.getByAccountId(accountId));
    }

    @PostMapping
    public ResponseEntity<OpeningBalance> create(@RequestBody OpeningBalance ob) {
        return ResponseEntity.ok(service.saveOpeningBalance(ob));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteOpeningBalance(id);
        return ResponseEntity.noContent().build();
    }
}
