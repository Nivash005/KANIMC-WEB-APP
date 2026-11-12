package com.kanimc.repository;

import com.kanimc.entity.FinancialYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FinancialYearRepository extends JpaRepository<FinancialYear, Long> {
    Optional<FinancialYear> findByYearCode(String yearCode);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM FinancialYear f WHERE :date >= f.startDate AND :date <= f.endDate")
    Optional<FinancialYear> findByDate(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);
}
