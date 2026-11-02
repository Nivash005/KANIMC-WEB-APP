package com.kanimc.repository;

import com.kanimc.entity.ChittaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChittaItemRepository extends JpaRepository<ChittaItem, Long> {

    @Query("SELECT ci FROM ChittaItem ci WHERE " +
           "(:startDate IS NULL OR ci.chitta.chittaDate >= :startDate) AND " +
           "(:endDate IS NULL OR ci.chitta.chittaDate <= :endDate) AND " +
           "(:itemId IS NULL OR ci.item.id = :itemId)")
    List<ChittaItem> findFiltered(@Param("startDate") LocalDate startDate, 
                                  @Param("endDate") LocalDate endDate, 
                                  @Param("itemId") Long itemId);

    @Query("SELECT new com.kanimc.dto.ItemSummaryDTO(ci.item.id, ci.item.des, SUM(ci.quantity), SUM(ci.amount)) " +
           "FROM ChittaItem ci WHERE " +
           "(:startDate IS NULL OR ci.chitta.chittaDate >= :startDate) AND " +
           "(:endDate IS NULL OR ci.chitta.chittaDate <= :endDate) " +
           "GROUP BY ci.item.id, ci.item.des")
    List<com.kanimc.dto.ItemSummaryDTO> getItemSummary(@Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT ci.chitta.id, SUM(ci.quantity) FROM ChittaItem ci WHERE ci.chitta.id IN :chittaIds GROUP BY ci.chitta.id")
    List<Object[]> findTotalWeightByChittaIds(@Param("chittaIds") List<Long> chittaIds);

    @Query("SELECT ci.chitta.id, SUM(ci.quantity) FROM ChittaItem ci " +
           "WHERE (:startDate IS NULL OR ci.chitta.chittaDate >= :startDate) " +
           "AND (:endDate IS NULL OR ci.chitta.chittaDate <= :endDate) " +
           "AND (:accountId IS NULL OR ci.chitta.farmerAccount.id = :accountId) " +
           "GROUP BY ci.chitta.id")
    List<Object[]> findTotalWeightsFiltered(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("accountId") Long accountId);
}
