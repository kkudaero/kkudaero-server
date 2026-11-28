package com.kkudaero.server.domain.checklist.entity;

import com.kkudaero.server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checklist_completions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistCompletion extends BaseTimeEntity {

    private static final int STEP_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = STEP_MAX_LENGTH)
    private ChecklistStep step;

    private ChecklistCompletion(Long sellerId, ChecklistStep step) {
        this.sellerId = sellerId;
        this.step = step;
    }

    public static ChecklistCompletion create(Long sellerId, ChecklistStep step) {
        return new ChecklistCompletion(sellerId, step);
    }
}
