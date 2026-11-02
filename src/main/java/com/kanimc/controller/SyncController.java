package com.kanimc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    private final JdbcTemplate jdbcTemplate;
    public static final AtomicLong version = new AtomicLong(System.currentTimeMillis());

    public SyncController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static void bumpVersion() {
        version.set(System.currentTimeMillis());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", version.get());
        try {
            Long chittaCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chittas", Long.class);
            Long paidCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chittas WHERE is_paid = true", Long.class);
            Long accountCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts", Long.class);
            Long cashCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cash_vouchers", Long.class);
            Long receiptCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM receipt_vouchers", Long.class);
            
            map.put("chittaCount", chittaCount != null ? chittaCount : 0L);
            map.put("paidCount", paidCount != null ? paidCount : 0L);
            map.put("accountCount", accountCount != null ? accountCount : 0L);
            map.put("cashCount", cashCount != null ? cashCount : 0L);
            map.put("receiptCount", receiptCount != null ? receiptCount : 0L);
            map.put("status", "ONLINE");
        } catch (Exception e) {
            map.put("status", "OK");
        }
        return ResponseEntity.ok(map);
    }

    @PostMapping("/bump")
    public ResponseEntity<Void> triggerBump() {
        bumpVersion();
        return ResponseEntity.ok().build();
    }
}
