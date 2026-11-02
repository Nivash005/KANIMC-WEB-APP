package com.kanimc.repository;

import com.kanimc.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.kanimc.dto.OutstandingDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction> findByAccountIdOrderByTransactionDateAsc(Long accountId);
    
    List<AccountTransaction> findByTransactionDate(LocalDate transactionDate);

    @Query("SELECT t FROM AccountTransaction t WHERE t.account.id = :accountId AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate ORDER BY t.transactionDate ASC")
    List<AccountTransaction> findFiltered(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("accountId") Long accountId);

    @Query("SELECT SUM(COALESCE(t.debitAmount, 0)) - SUM(COALESCE(t.creditAmount, 0)) FROM AccountTransaction t WHERE t.account.id = :accountId")
    BigDecimal calculateRunningBalance(@Param("accountId") Long accountId);
    
    @Query("SELECT new com.kanimc.dto.OutstandingDTO(t.account.id, t.account.accountName, SUM(COALESCE(t.debitAmount, 0)) - SUM(COALESCE(t.creditAmount, 0))) FROM AccountTransaction t WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate GROUP BY t.account.id, t.account.accountName HAVING (SUM(COALESCE(t.debitAmount, 0)) - SUM(COALESCE(t.creditAmount, 0))) <> 0")
    List<OutstandingDTO> getOutstandingReport(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM AccountTransaction t WHERE t.docCode = 3 AND t.debitAmount > 0 " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "AND (:accountId IS NULL OR t.account.id = :accountId)")
    List<AccountTransaction> findChittaPayments(@Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate, 
                                                @Param("accountId") Long accountId);

    void deleteByDocCodeAndDocNo(Integer docCode, String docNo);
}
