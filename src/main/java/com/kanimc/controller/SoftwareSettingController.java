package com.kanimc.controller;

import com.kanimc.entity.SoftwareSetting;
import com.kanimc.repository.SoftwareSettingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class SoftwareSettingController {
    private final SoftwareSettingRepository repository;

    public SoftwareSettingController(SoftwareSettingRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<SoftwareSetting> getSettings() {
        List<SoftwareSetting> list = repository.findAll();
        if (list.isEmpty()) {
            SoftwareSetting defaultSettings = new SoftwareSetting();
            defaultSettings.setZiscom(true);
            return ResponseEntity.ok(defaultSettings);
        }
        return ResponseEntity.ok(list.get(0));
    }

    @PostMapping
    public ResponseEntity<SoftwareSetting> saveSettings(@RequestBody SoftwareSetting settings) {
        List<SoftwareSetting> list = repository.findAll();
        if (!list.isEmpty()) {
            settings.setId(list.get(0).getId());
        }
        return ResponseEntity.ok(repository.save(settings));
    }
}
