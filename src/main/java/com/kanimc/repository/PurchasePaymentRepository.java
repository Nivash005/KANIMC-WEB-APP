package com.kanimc.repository;

import com.kanimc.entity.PurchasePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, Long> {
    @Query("SELECT pp FROM PurchasePayment pp LEFT JOIN FETCH pp.traderAccount WHERE " +
           "(:startDate IS NULL OR pp.paymentDate >= :startDate) AND " +
           "(:endDate IS NULL OR pp.paymentDate <= :endDate) AND " +
           "(:accountId IS NULL OR pp.traderAccount.id = :accountId) ORDER BY pp.paymentDate ASC")
    List<PurchasePayment> findFiltered(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate, 
                                       @Param("accountId") Long accountId);
}
