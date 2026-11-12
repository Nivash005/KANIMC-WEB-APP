package com.kanimc.repository;
import com.kanimc.entity.FinancialPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, Long> {
    Optional<FinancialPeriod> findByPeriodName(String periodName);
}
