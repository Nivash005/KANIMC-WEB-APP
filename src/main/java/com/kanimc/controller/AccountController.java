package com.kanimc.controller;

import com.kanimc.entity.Account;
import com.kanimc.repository.AccountRepository;
import com.kanimc.service.AccountingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*") // Allow frontend to call APIs
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final com.kanimc.repository.AccountGroupRepository accountGroupRepository;

    public AccountController(AccountRepository accountRepository, AccountingService accountingService, com.kanimc.repository.AccountGroupRepository accountGroupRepository) {
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.accountGroupRepository = accountGroupRepository;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody java.util.Map<String, String> data) {
        Account account = new Account();
        account.setPartyId("P-" + System.currentTimeMillis());
        account.setAccountName(data.get("accountName"));
        account.setRegionalName(data.get("place"));
        
        String groupName = data.get("group") != null ? data.get("group") : "GENERAL";
        Integer groupCode = Math.abs(groupName.hashCode());
        com.kanimc.entity.AccountGroup group = accountGroupRepository.findByGroupCode(groupCode).orElseGet(() -> {
            com.kanimc.entity.AccountGroup g = new com.kanimc.entity.AccountGroup();
            g.setGroupCode(groupCode);
            g.setGroupName(groupName);
            return accountGroupRepository.save(g);
        });
        account.setAccountGroup(group);
        
        try {
            return ResponseEntity.ok(accountRepository.save(account));
        } catch(Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long id) {
        try {
            BigDecimal balance = accountingService.getAccountBalance(id);
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody java.util.Map<String, String> data) {
        return accountRepository.findById(id)
                .map(account -> {
                    account.setAccountName(data.get("accountName"));
                    account.setRegionalName(data.get("place"));
                    String groupName = data.get("group") != null ? data.get("group") : "GENERAL";
                    Integer groupCode = Math.abs(groupName.hashCode());
                    com.kanimc.entity.AccountGroup group = accountGroupRepository.findByGroupCode(groupCode).orElseGet(() -> {
                        com.kanimc.entity.AccountGroup g = new com.kanimc.entity.AccountGroup();
                        g.setGroupCode(groupCode);
                        g.setGroupName(groupName);
                        return accountGroupRepository.save(g);
                    });
                    account.setAccountGroup(group);
                    return ResponseEntity.ok(accountRepository.save(account));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        if (accountRepository.existsById(id)) {
            accountRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
