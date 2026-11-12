package com.kanimc.controller;

import com.kanimc.entity.DailyCashOpening;
import com.kanimc.repository.DailyCashOpeningRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-cash-opening")
public class DailyCashOpeningController {
    private final DailyCashOpeningRepository repository;

    public DailyCashOpeningController(DailyCashOpeningRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<DailyCashOpening>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/by-date")
    public ResponseEntity<DailyCashOpening> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return repository.findByBalanceDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DailyCashOpening> save(@RequestBody DailyCashOpening dbal) {
        if (dbal.getBalanceDate() == null) {
            dbal.setBalanceDate(LocalDate.now());
        }
        DailyCashOpening existing = repository.findByBalanceDate(dbal.getBalanceDate()).orElse(null);
        if (existing != null && dbal.getId() == null) {
            dbal.setId(existing.getId());
        }
        return ResponseEntity.ok(repository.save(dbal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
