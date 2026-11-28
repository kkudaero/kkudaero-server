package com.kkudaero.server.domain.checklist.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.kkudaero.server.domain.checklist.dto.ChecklistStepResponse;
import com.kkudaero.server.domain.checklist.entity.ChecklistCompletion;
import com.kkudaero.server.domain.checklist.entity.ChecklistStep;
import com.kkudaero.server.domain.checklist.repository.ChecklistCompletionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistCompletionRepository checklistCompletionRepository;

    @Transactional(readOnly = true)
    public List<ChecklistStepResponse> getChecklistSteps(Long sellerId) {
        Map<ChecklistStep, ChecklistCompletion> completions = checklistCompletionRepository.findBySellerId(sellerId)
                .stream()
                .collect(Collectors.toMap(ChecklistCompletion::getStep, Function.identity()));

        return Arrays.stream(ChecklistStep.values())
                .sorted(Comparator.comparingInt(ChecklistStep::getOrder))
                .map(step -> Optional.ofNullable(completions.get(step))
                        .map(ChecklistStepResponse::completed)
                        .orElseGet(() -> ChecklistStepResponse.notCompleted(step)))
                .toList();
    }

    @Transactional
    public ChecklistStepResponse updateChecklistStep(Long sellerId, ChecklistStep step, boolean isCompleted) {
        Optional<ChecklistCompletion> completion = checklistCompletionRepository.findBySellerIdAndStep(sellerId, step);
        if (!isCompleted) {
            completion.ifPresent(checklistCompletionRepository::delete);
            return ChecklistStepResponse.notCompleted(step);
        }
        return ChecklistStepResponse.completed(
                completion.orElseGet(
                        () -> checklistCompletionRepository.save(ChecklistCompletion.create(sellerId, step))
                )
        );
    }
}
