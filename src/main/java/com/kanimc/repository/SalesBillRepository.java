package com.kanimc.repository;
import com.kanimc.entity.SalesBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesBillRepository extends JpaRepository<SalesBill, Long> {
    @Query("SELECT b FROM SalesBill b WHERE " +
           "(:startDate IS NULL OR b.billDate >= :startDate) AND " +
           "(:endDate IS NULL OR b.billDate <= :endDate) AND " +
           "(:accountId IS NULL OR b.buyerAccount.id = :accountId)")
    List<SalesBill> findFiltered(@Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate, 
                                 @Param("accountId") Long accountId);
}
