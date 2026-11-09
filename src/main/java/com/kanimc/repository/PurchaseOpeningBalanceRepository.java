package com.kanimc.repository;

import com.kanimc.entity.PurchaseOpeningBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOpeningBalanceRepository extends JpaRepository<PurchaseOpeningBalance, Long> {
    List<PurchaseOpeningBalance> findByTraderAccountId(Long traderAccountId);
}
