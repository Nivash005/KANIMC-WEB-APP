package com.kanimc.repository;

import com.kanimc.entity.Chitta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChittaRepository extends JpaRepository<Chitta, Long> {
    Optional<Chitta> findFirstByChittaNoOrderByChittaDateDesc(String chittaNo);

    List<Chitta> findAllByChittaNo(String chittaNo);

    Optional<Chitta> findByChittaDateAndChittaNo(LocalDate chittaDate, String chittaNo);

    @Query("SELECT c.chittaNo FROM Chitta c WHERE c.chittaDate = :date")
    List<String> findChittaNosByDate(@Param("date") LocalDate date);

    @Query("SELECT c FROM Chitta c LEFT JOIN FETCH c.farmerAccount WHERE " +
           "(:startDate IS NULL OR c.chittaDate >= :startDate) AND " +
           "(:endDate IS NULL OR c.chittaDate <= :endDate) AND " +
           "(:accountId IS NULL OR c.farmerAccount.id = :accountId) AND " +
           "(c.del IS NULL OR c.del <> 'Y') " +
           "ORDER BY c.chittaDate ASC, c.chittaNo ASC")
    List<Chitta> findFiltered(@Param("startDate") LocalDate startDate, 
                              @Param("endDate") LocalDate endDate, 
                              @Param("accountId") Long accountId);

    @Query("SELECT c FROM Chitta c LEFT JOIN FETCH c.farmerAccount LEFT JOIN FETCH c.items WHERE " +
           "(:startDate IS NULL OR c.chittaDate >= :startDate) AND " +
           "(:endDate IS NULL OR c.chittaDate <= :endDate) AND " +
           "(:accountId IS NULL OR c.farmerAccount.id = :accountId) AND " +
           "(c.del IS NULL OR c.del <> 'Y') " +
           "ORDER BY c.chittaDate ASC, c.chittaNo ASC")
    List<Chitta> findFilteredForRegister(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate, 
                                         @Param("accountId") Long accountId);

    @Query("SELECT c FROM Chitta c LEFT JOIN FETCH c.farmerAccount LEFT JOIN FETCH c.items WHERE " +
           "(c.del IS NULL OR c.del <> 'Y') AND " +
           "c.chittaDate >= :startDate ORDER BY c.chittaDate ASC, c.chittaNo ASC")
    List<Chitta> findByYearFrom(@Param("startDate") LocalDate startDate);

    @Query("SELECT c FROM Chitta c LEFT JOIN FETCH c.farmerAccount WHERE " +
           "(c.del IS NULL OR c.del <> 'Y') AND " +
           "(c.paid = true OR c.payDate IS NOT NULL) AND " +
           "(:startDate IS NULL OR COALESCE(c.payDate, c.chittaDate) >= :startDate) AND " +
           "(:endDate IS NULL OR COALESCE(c.payDate, c.chittaDate) <= :endDate) AND " +
           "(:accountId IS NULL OR c.farmerAccount.id = :accountId) " +
           "ORDER BY COALESCE(c.payDate, c.chittaDate) ASC, c.chittaNo ASC")
    List<Chitta> findPaidFiltered(@Param("startDate") LocalDate startDate, 
                                  @Param("endDate") LocalDate endDate, 
                                  @Param("accountId") Long accountId);

    @Query("SELECT c FROM Chitta c LEFT JOIN FETCH c.farmerAccount WHERE " +
           "(c.del IS NULL OR c.del <> 'Y') AND " +
           "(c.paid IS NULL OR c.paid = false) AND c.payDate IS NULL AND " +
           "(:asOfDate IS NULL OR c.chittaDate <= :asOfDate) AND " +
           "(:accountId IS NULL OR c.farmerAccount.id = :accountId) " +
           "ORDER BY c.chittaDate ASC, c.chittaNo ASC")
    List<Chitta> findUnpaidFiltered(@Param("asOfDate") LocalDate asOfDate, 
                                    @Param("accountId") Long accountId);
}
