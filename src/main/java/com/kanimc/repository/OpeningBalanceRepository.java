package com.kanimc.repository;

import com.kanimc.entity.OpeningBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OpeningBalanceRepository extends JpaRepository<OpeningBalance, Long> {
    List<OpeningBalance> findByAccountId(Long accountId);
}
