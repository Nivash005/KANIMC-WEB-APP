package com.kanimc.controller;

import com.kanimc.entity.CommissionRate;
import com.kanimc.service.CommissionRateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/commission-rates")
public class CommissionRateController {
    private final CommissionRateService service;

    public CommissionRateController(CommissionRateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CommissionRate>> getAll() {
        return ResponseEntity.ok(service.getAllRates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommissionRate> getById(@PathVariable Long id) {
        return service.getRateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/effective")
    public ResponseEntity<BigDecimal> getEffectiveRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.getEffectiveRate(date));
    }

    @PostMapping
    public ResponseEntity<CommissionRate> create(@RequestBody CommissionRate rate) {
        return ResponseEntity.ok(service.saveRate(rate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteRate(id);
        return ResponseEntity.noContent().build();
    }
}
