package com.kanimc.repository;

import com.kanimc.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByPurchaseNo(String purchaseNo);

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.traderAccount WHERE " +
           "(:startDate IS NULL OR p.purchaseDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.purchaseDate <= :endDate) AND " +
           "(:accountId IS NULL OR p.traderAccount.id = :accountId) ORDER BY p.purchaseDate ASC, p.purchaseNo ASC")
    List<Purchase> findFiltered(@Param("startDate") LocalDate startDate, 
                                @Param("endDate") LocalDate endDate, 
                                @Param("accountId") Long accountId);

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.traderAccount WHERE " +
           "(p.paid = false OR p.paid IS NULL) AND " +
           "(:accountId IS NULL OR p.traderAccount.id = :accountId) ORDER BY p.purchaseDate ASC, p.purchaseNo ASC")
    List<Purchase> findPending(@Param("accountId") Long accountId);
}
