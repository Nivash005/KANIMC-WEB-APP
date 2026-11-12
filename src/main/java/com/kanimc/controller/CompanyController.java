package com.kanimc.controller;

import com.kanimc.entity.Company;
import com.kanimc.repository.CompanyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public ResponseEntity<Company> getCompany() {
        List<Company> list = companyRepository.findAll();
        if (list.isEmpty()) {
            return ResponseEntity.ok(new Company());
        }
        return ResponseEntity.ok(list.get(0));
    }

    @PostMapping
    public ResponseEntity<Company> saveCompany(@RequestBody Company company) {
        List<Company> list = companyRepository.findAll();
        if (!list.isEmpty()) {
            company.setId(list.get(0).getId());
        }
        return ResponseEntity.ok(companyRepository.save(company));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        company.setId(id);
        return ResponseEntity.ok(companyRepository.save(company));
    }
}
