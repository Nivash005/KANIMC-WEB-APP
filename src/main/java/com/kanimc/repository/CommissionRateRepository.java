package com.kanimc.repository;

import com.kanimc.entity.CommissionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CommissionRateRepository extends JpaRepository<CommissionRate, Long> {
    Optional<CommissionRate> findFirstByWefdateLessThanEqualOrderByWefdateDesc(LocalDate date);
}
