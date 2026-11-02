package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.entity.Chitta;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.ItemRepository;
import com.kanimc.service.ChittaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/chittas")
@CrossOrigin(origins = "*")
public class ChittaController {
    private final ChittaService chittaService;
    private final AccountRepository accountRepository;
    private final ItemRepository itemRepository;

    public ChittaController(ChittaService chittaService, AccountRepository accountRepository, ItemRepository itemRepository) {
        this.chittaService = chittaService;
        this.accountRepository = accountRepository;
        this.itemRepository = itemRepository;
    }

    @PostMapping
    public ResponseEntity<?> createChitta(@RequestBody Chitta chitta,
                                          @RequestParam(required = false) Long cashAccountId,
                                          @RequestParam(required = false) Long commissionAccountId) {
        try {
            hydrateReferences(chitta);
            Account cash = cashAccountId == null ? null : accountRepository.findById(cashAccountId).orElseThrow(() -> new IllegalArgumentException("Cash account not found"));
            Account commission = commissionAccountId == null ? null : accountRepository.findById(commissionAccountId).orElseThrow(() -> new IllegalArgumentException("Commission account not found"));
            Chitta res = chittaService.saveChitta(chitta, cash, commission); SyncController.bumpVersion(); return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Unable to save Chitta"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateChitta(@PathVariable Long id, @RequestBody Chitta chitta,
                                          @RequestParam(required = false) Long cashAccountId,
                                          @RequestParam(required = false) Long commissionAccountId) {
        try {
            hydrateReferences(chitta);
            Account cash = cashAccountId == null ? null : accountRepository.findById(cashAccountId).orElseThrow(() -> new IllegalArgumentException("Cash account not found"));
            Account commission = commissionAccountId == null ? null : accountRepository.findById(commissionAccountId).orElseThrow(() -> new IllegalArgumentException("Commission account not found"));
            Chitta res = chittaService.updateChitta(id, chitta, cash, commission); SyncController.bumpVersion(); return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Unable to update Chitta"));
        }
    }

    @GetMapping
    public ResponseEntity<Iterable<Chitta>> getChittas(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        return ResponseEntity.ok(chittaService.getFilteredChittas(startDate, endDate, accountId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Chitta> getChittaById(@PathVariable Long id) {
        return chittaService.getChittaById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/next-no")
    public ResponseEntity<?> getNextChittaNo(@RequestParam(required = false) LocalDate date) {
        String nextNo = chittaService.getNextChittaNo(date != null ? date : LocalDate.now());
        return ResponseEntity.ok(java.util.Map.of("nextNo", nextNo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChitta(@PathVariable Long id) {
        try { chittaService.deleteChitta(id); SyncController.bumpVersion(); return ResponseEntity.noContent().build(); }
        catch (IllegalArgumentException e) { return ResponseEntity.notFound().build(); }
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<?> recordPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String paidTo,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate payDate) {
        try {
            Chitta updated = chittaService.recordPayment(id, paidTo, paymentMethod, payDate);
            SyncController.bumpVersion();
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Unable to record payment: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/payment")
    public ResponseEntity<?> clearPayment(@PathVariable Long id) {
        try {
            Chitta updated = chittaService.clearPayment(id);
            SyncController.bumpVersion();
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Unable to clear payment: " + e.getMessage()));
        }
    }

    private void hydrateReferences(Chitta chitta) {
        if (chitta.getFarmerAccount() == null || chitta.getFarmerAccount().getId() == null)
            throw new IllegalArgumentException("Farmer account is required");
        chitta.setFarmerAccount(accountRepository.findById(chitta.getFarmerAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Farmer account not found")));
        if (chitta.getItems() != null) {
            chitta.getItems().forEach(row -> {
                if (row.getItem() == null || row.getItem().getId() == null)
                    throw new IllegalArgumentException("Item is required for every row");
                row.setItem(itemRepository.findById(row.getItem().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Item not found: " + row.getItem().getId())));
            });
        }
    }
}
