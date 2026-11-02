package com.kanimc.repository;

import com.kanimc.entity.ReceiptVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReceiptVoucherRepository extends JpaRepository<ReceiptVoucher, Long> {
    @Query("SELECT r FROM ReceiptVoucher r WHERE " +
           "(:startDate IS NULL OR r.voucherDate >= :startDate) AND " +
           "(:endDate IS NULL OR r.voucherDate <= :endDate) AND " +
           "(:accountId IS NULL OR r.account.id = :accountId)")
    List<ReceiptVoucher> findFiltered(@Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate, 
                                      @Param("accountId") Long accountId);
}
