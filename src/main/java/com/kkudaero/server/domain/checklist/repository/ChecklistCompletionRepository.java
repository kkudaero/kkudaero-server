package com.kkudaero.server.domain.checklist.repository;

import java.util.List;
import java.util.Optional;

import com.kkudaero.server.domain.checklist.entity.ChecklistCompletion;
import com.kkudaero.server.domain.checklist.entity.ChecklistStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChecklistCompletionRepository extends JpaRepository<ChecklistCompletion, Long> {

    List<ChecklistCompletion> findBySellerId(Long sellerId);

    Optional<ChecklistCompletion> findBySellerIdAndStep(Long sellerId, ChecklistStep step);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ChecklistCompletion e where e.sellerId = :sellerId")
    void deleteAllBySellerId(@Param("sellerId") Long sellerId);
}
