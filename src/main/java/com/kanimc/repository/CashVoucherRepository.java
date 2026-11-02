package com.kanimc.repository;

import com.kanimc.entity.CashVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CashVoucherRepository extends JpaRepository<CashVoucher, Long> {
    @Query("SELECT v FROM CashVoucher v LEFT JOIN FETCH v.account WHERE " +
           "(:startDate IS NULL OR v.voucherDate >= :startDate) AND " +
           "(:endDate IS NULL OR v.voucherDate <= :endDate) AND " +
           "(:accountId IS NULL OR v.account.id = :accountId)")
    List<CashVoucher> findFiltered(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate, 
                                   @Param("accountId") Long accountId);
}
