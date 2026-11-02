package com.kanimc.repository;

import com.kanimc.entity.DailyCashOpening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyCashOpeningRepository extends JpaRepository<DailyCashOpening, Long> {
    Optional<DailyCashOpening> findByBalanceDate(LocalDate balanceDate);
}
