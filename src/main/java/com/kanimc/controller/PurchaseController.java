package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.entity.Purchase;
import com.kanimc.entity.PurchaseItem;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.ItemRepository;
import com.kanimc.repository.PurchaseRepository;
import com.kanimc.service.AccountingService;
import com.kanimc.service.AccountMappingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*")
public class PurchaseController {

    private final PurchaseRepository purchaseRepository;
    private final AccountRepository accountRepository;
    private final ItemRepository itemRepository;
    private final AccountingService accountingService;
    private final AccountMappingService accountMappingService;

    public PurchaseController(PurchaseRepository purchaseRepository,
                              AccountRepository accountRepository,
                              ItemRepository itemRepository,
                              AccountingService accountingService,
                              AccountMappingService accountMappingService) {
        this.purchaseRepository = purchaseRepository;
        this.accountRepository = accountRepository;
        this.itemRepository = itemRepository;
        this.accountingService = accountingService;
        this.accountMappingService = accountMappingService;
    }

    @GetMapping
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    @GetMapping("/next-no")
    public java.util.Map<String, Object> getNextPurchaseNo() {
        List<Purchase> all = purchaseRepository.findAll();
        long max = 0;
        for (Purchase p : all) {
            try {
                if (p.getPurchaseNo() != null) {
                    long n = Long.parseLong(p.getPurchaseNo().trim());
                    if (n > max) max = n;
                }
            } catch (Exception ignored) {}
        }
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("nextNo", max + 1);
        return map;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Purchase> getPurchaseById(@PathVariable Long id) {
        return purchaseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    public List<Purchase> getPendingPurchases(@RequestParam(required = false) Long accountId) {
        return purchaseRepository.findPending(accountId);
    }

    @PostMapping
    public ResponseEntity<Purchase> createPurchase(@RequestBody Purchase purchase) {
        if (purchase.getPurchaseDate() == null) {
            purchase.setPurchaseDate(LocalDate.now());
        }

        // Resolve trader account
        if (purchase.getTraderAccount() != null && purchase.getTraderAccount().getId() != null) {
            Account fullTrader = accountRepository.findById(purchase.getTraderAccount().getId()).orElse(null);
            if (fullTrader != null) purchase.setTraderAccount(fullTrader);
        }

        // Link items
        if (purchase.getItems() != null) {
            for (PurchaseItem it : purchase.getItems()) {
                it.setPurchase(purchase);
                if (it.getItem() != null && it.getItem().getId() != null) {
                    it.setItem(itemRepository.findById(it.getItem().getId()).orElse(it.getItem()));
                }
            }
        }

        Purchase saved = purchaseRepository.save(purchase);

        // Double-entry: DR Purchase Account, CR Trader Account
        Long purchaseAccountId = accountMappingService.getAccountId("PURCHASE", 3L);
        Account purchaseAcc = null;
        if (purchaseAccountId != null) {
            purchaseAcc = accountRepository.findById(purchaseAccountId).orElse(null);
        }
        if (purchaseAcc == null) {
            purchaseAcc = accountRepository.findByAccountNameIgnoreCase("PURCHASE").orElse(null);
        }
        if (purchaseAcc == null) {
            purchaseAcc = accountRepository.findById(3L).orElse(null);
        }

        Account traderAcc = saved.getTraderAccount();
        BigDecimal net = saved.getNetPayable() != null ? saved.getNetPayable() : saved.getGrossAmount();

        if (purchaseAcc != null && traderAcc != null && net != null && net.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postDoubleEntry(
                saved.getPurchaseDate(),
                purchaseAcc, // DR Purchase Account
                traderAcc,   // CR Trader Account
                net,
                5,           // Doc Code 5 = Purchase
                saved.getPurchaseNo(),
                saved.getFinancialYear(),
                "Purchase " + saved.getPurchaseNo()
            );
        }

        SyncController.bumpVersion();
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Purchase> updatePurchase(@PathVariable Long id, @RequestBody Purchase purchase) {
        Purchase existing = purchaseRepository.findById(id).orElse(null);
        if (existing == null && purchase.getPurchaseNo() != null) {
            existing = purchaseRepository.findByPurchaseNo(purchase.getPurchaseNo()).orElse(null);
        }

        if (existing != null) {
            if (purchase.getPurchaseDate() != null) existing.setPurchaseDate(purchase.getPurchaseDate());
            if (purchase.getPurchaseNo() != null) existing.setPurchaseNo(purchase.getPurchaseNo());
            if (purchase.getGrossAmount() != null) existing.setGrossAmount(purchase.getGrossAmount());
            if (purchase.getFreightCharges() != null) existing.setFreightCharges(purchase.getFreightCharges());
            if (purchase.getPackingCharges() != null) existing.setPackingCharges(purchase.getPackingCharges());
            if (purchase.getOtherCharges() != null) existing.setOtherCharges(purchase.getOtherCharges());
            if (purchase.getDiscount() != null) existing.setDiscount(purchase.getDiscount());
            if (purchase.getNetPayable() != null) existing.setNetPayable(purchase.getNetPayable());
            if (purchase.getPaid() != null) existing.setPaid(purchase.getPaid());
            if (purchase.getPaidAmount() != null) existing.setPaidAmount(purchase.getPaidAmount());
            if (purchase.getPayDate() != null) existing.setPayDate(purchase.getPayDate());

            if (purchase.getTraderAccount() != null && purchase.getTraderAccount().getId() != null) {
                Account fullTrader = accountRepository.findById(purchase.getTraderAccount().getId()).orElse(null);
                if (fullTrader != null) existing.setTraderAccount(fullTrader);
            }

            if (purchase.getItems() != null) {
                existing.getItems().clear();
                for (PurchaseItem it : purchase.getItems()) {
                    it.setPurchase(existing);
                    if (it.getItem() != null && it.getItem().getId() != null) {
                        it.setItem(itemRepository.findById(it.getItem().getId()).orElse(it.getItem()));
                    }
                    existing.getItems().add(it);
                }
            }

            Purchase saved = purchaseRepository.save(existing);
            SyncController.bumpVersion();
            return ResponseEntity.ok(saved);
        } else {
            return createPurchase(purchase);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id) {
        if (purchaseRepository.existsById(id)) {
            purchaseRepository.deleteById(id);
            SyncController.bumpVersion();
        }
        return ResponseEntity.noContent().build();
    }
}
