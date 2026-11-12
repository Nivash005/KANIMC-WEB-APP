package com.kanimc.controller;

import com.kanimc.entity.AccountGroup;
import com.kanimc.repository.AccountGroupRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/account-groups")
public class AccountGroupController {
    private final AccountGroupRepository repository;

    public AccountGroupController(AccountGroupRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<AccountGroup>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountGroup> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AccountGroup> create(@RequestBody AccountGroup group) {
        if (group.getGroupCode() == null) {
            group.setGroupCode((int) repository.count() + 1);
        }
        return ResponseEntity.ok(repository.save(group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountGroup> update(@PathVariable Long id, @RequestBody AccountGroup group) {
        return repository.findById(id)
                .map(existing -> {
                    if (group.getGroupName() != null) existing.setGroupName(group.getGroupName());
                    if (group.getAgtype() != null) existing.setAgtype(group.getAgtype());
                    if (group.getArea() != null) existing.setArea(group.getArea());
                    if (group.getAgout() != null) existing.setAgout(group.getAgout());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
