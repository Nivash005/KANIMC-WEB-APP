package com.kanimc.service;
import com.kanimc.entity.AccountMapping;
import com.kanimc.repository.AccountMappingRepository;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountMappingService {
    private final AccountMappingRepository repo;
    public AccountMappingService(AccountMappingRepository repo) { this.repo = repo; }
    public Long getAccountId(String type, Long defaultId) {
        return repo.findById(type).map(AccountMapping::getAccountId).orElse(defaultId);
    }
    public void setMapping(String type, Long accountId) {
        repo.save(new AccountMapping(type, accountId));
    }
    public Map<String, Long> getAllMappings() {
        return repo.findAll().stream().collect(Collectors.toMap(AccountMapping::getMappingType, AccountMapping::getAccountId));
    }
}
