package com.kkudaero.server.domain.checklist.repository;

import java.util.List;
import java.util.Optional;

import com.kkudaero.server.domain.checklist.entity.ChecklistCompletion;
import com.kkudaero.server.domain.checklist.entity.ChecklistStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistCompletionRepository extends JpaRepository<ChecklistCompletion, Long> {

    List<ChecklistCompletion> findBySellerId(Long sellerId);

    Optional<ChecklistCompletion> findBySellerIdAndStep(Long sellerId, ChecklistStep step);
}
