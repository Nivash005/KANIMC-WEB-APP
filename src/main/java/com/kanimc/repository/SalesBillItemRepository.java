package com.kanimc.repository;

import com.kanimc.entity.SalesBillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesBillItemRepository extends JpaRepository<SalesBillItem, Long> {
    @Query("SELECT COALESCE(it.des, 'SEDI'), SUM(i.quantity), SUM(i.amount) FROM SalesBillItem i LEFT JOIN i.item it WHERE " +
           "(:startDate IS NULL OR i.salesBill.billDate >= :startDate) AND " +
           "(:endDate IS NULL OR i.salesBill.billDate <= :endDate) " +
           "GROUP BY COALESCE(it.des, 'SEDI')")
    List<Object[]> getItemSummary(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT i FROM SalesBillItem i JOIN FETCH i.salesBill b LEFT JOIN FETCH i.item it LEFT JOIN FETCH b.buyerAccount a WHERE " +
           "(:startDate IS NULL OR b.billDate >= :startDate) AND " +
           "(:endDate IS NULL OR b.billDate <= :endDate) " +
           "ORDER BY b.billDate ASC, b.billNo ASC, i.id ASC")
    List<SalesBillItem> findSalesRegister(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT i FROM SalesBillItem i JOIN FETCH i.salesBill b LEFT JOIN FETCH i.item it LEFT JOIN FETCH b.buyerAccount a WHERE " +
           "(:startDate IS NULL OR b.billDate >= :startDate) AND " +
           "(:endDate IS NULL OR b.billDate <= :endDate) AND " +
           "(:itemId IS NULL OR (it IS NOT NULL AND it.id = :itemId)) " +
           "ORDER BY b.billDate ASC, b.billNo ASC, i.id ASC")
    List<SalesBillItem> findItemRegister(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate, 
                                         @Param("itemId") Long itemId);
}
