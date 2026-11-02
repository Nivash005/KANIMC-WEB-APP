package com.kanimc.repository;

import com.kanimc.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByPartyId(String partyId);
    Optional<Account> findByAccountNameIgnoreCase(String accountName);
}
