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
    private final com.kanimc.repository.AccountGroupRepository accountGroupRepository;

    public ChittaController(ChittaService chittaService, AccountRepository accountRepository, ItemRepository itemRepository, com.kanimc.repository.AccountGroupRepository accountGroupRepository) {
        this.chittaService = chittaService;
        this.accountRepository = accountRepository;
        this.itemRepository = itemRepository;
        this.accountGroupRepository = accountGroupRepository;
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
            if (paidTo == null || paidTo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Paid to is required and cannot be empty."));
            }
            Chitta updated = chittaService.recordPayment(id, paidTo.trim(), paymentMethod, payDate);
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
        if (chitta.getFarmerAccount() == null && (chitta.getPaidTo() == null || chitta.getPaidTo().trim().isEmpty())) {
            throw new IllegalArgumentException("Farmer account is required");
        }
        com.kanimc.entity.Account account = null;
        if (chitta.getFarmerAccount() != null && chitta.getFarmerAccount().getId() != null) {
            account = accountRepository.findById(chitta.getFarmerAccount().getId()).orElse(null);
        }
        if (account == null && chitta.getFarmerAccount() != null && chitta.getFarmerAccount().getPartyId() != null && !chitta.getFarmerAccount().getPartyId().trim().isEmpty()) {
            account = accountRepository.findByPartyId(chitta.getFarmerAccount().getPartyId().trim());
        }
        if (account == null && chitta.getFarmerAccount() != null && chitta.getFarmerAccount().getAccountName() != null && !chitta.getFarmerAccount().getAccountName().trim().isEmpty()) {
            account = accountRepository.findByAccountNameIgnoreCase(chitta.getFarmerAccount().getAccountName().trim()).orElse(null);
        }
        if (account == null) {
            account = new com.kanimc.entity.Account();
            String fName = (chitta.getFarmerAccount() != null && chitta.getFarmerAccount().getAccountName() != null && !chitta.getFarmerAccount().getAccountName().trim().isEmpty())
                ? chitta.getFarmerAccount().getAccountName().trim()
                : "GENERAL FARMER";
            account.setAccountName(fName);
            
            String pId = (chitta.getFarmerAccount() != null && chitta.getFarmerAccount().getPartyId() != null) 
                ? chitta.getFarmerAccount().getPartyId().trim() 
                : "";
            long nextCandidate = 2494;
            while (accountRepository.findByPartyId(String.valueOf(nextCandidate)) != null) {
                nextCandidate++;
            }
            if (pId.isEmpty() || accountRepository.findByPartyId(pId) != null) {
                pId = String.valueOf(nextCandidate > 9999 ? (nextCandidate % 10000) : nextCandidate);
            } else if (pId.length() > 4) {
                pId = pId.substring(0, 4);
            }
            account.setPartyId(pId);

            String groupName = "FARMER";
            Integer groupCode = Math.abs(groupName.hashCode());
            com.kanimc.entity.AccountGroup group = accountGroupRepository.findByGroupCode(groupCode).orElseGet(() -> {
                com.kanimc.entity.AccountGroup g = new com.kanimc.entity.AccountGroup();
                g.setGroupCode(groupCode);
                g.setGroupName(groupName);
                return accountGroupRepository.save(g);
            });
            account.setAccountGroup(group);
            account.setIsFarmer(true);
            account.setDel("   ");
            account.setCrby("SYS");
            account = accountRepository.save(account);
        }
        chitta.setFarmerAccount(account);

        // Paid To is NEVER automatically filled on Chitta creation or update.
        // It must remain null until explicitly settled in Chitta Payment.
        if (!Boolean.TRUE.equals(chitta.getPaid())) {
            chitta.setPaid(false);
            chitta.setPaidTo(null);
            chitta.setPayDate(null);
            chitta.setPaymentMethod(null);
        }

        if (chitta.getItems() != null) {
            chitta.getItems().forEach(row -> {
                com.kanimc.entity.Item item = null;
                if (row.getItem() != null && row.getItem().getId() != null) {
                    item = itemRepository.findById(row.getItem().getId()).orElse(null);
                }
                if (item == null) {
                    item = itemRepository.findAll().stream().findFirst().orElse(null);
                }
                if (item == null) {
                    item = new com.kanimc.entity.Item();
                    item.setDes("SEDI");
                    item.setDel("   ");
                    item.setCrby("SYS");
                    item = itemRepository.save(item);
                }
                row.setItem(item);
            });
        }
    }
}