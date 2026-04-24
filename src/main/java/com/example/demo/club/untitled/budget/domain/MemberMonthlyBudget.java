package com.example.demo.club.untitled.budget.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 멤버별 월별 기본 예산 한도 스냅샷.
 * - 최초 조회 시 {@code BudgetPolicy} 가 계산한 값을 lazy 저장 → 과거 월은 불변.
 * - 관리자 조정 시 {@link #adjust(int, String)} 로 한도 교체. 정책 변경은 과거 행에 영향 없음.
 */
@Entity
@Table(
    name = "member_monthly_budget",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_mmb_club_member_ym",
            // NOTE: 컬럼명은 target_month — MySQL 에서 `year_month` 는 interval unit reserved keyword 라 DDL 이 깨짐.
            columnNames = {"club_id", "member_id", "target_month"}
        )
    },
    indexes = {
        @Index(name = "idx_mmb_club_ym", columnList = "club_id, target_month")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MemberMonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** "YYYY-MM" 포맷. MySQL reserved keyword 회피를 위해 DB 컬럼명은 target_month. */
    @Column(name = "target_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "base_limit", nullable = false)
    private int baseLimit;

    @Column(name = "adjust_reason", length = 200)
    private String adjustReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static MemberMonthlyBudget of(Long clubId, Long memberId, YearMonth yearMonth, int baseLimit) {
        MemberMonthlyBudget b = new MemberMonthlyBudget();
        b.clubId = clubId;
        b.memberId = memberId;
        b.yearMonth = yearMonth.toString();
        b.baseLimit = baseLimit;
        return b;
    }

    public void adjust(int newLimit, String reason) {
        if (newLimit < 0) {
            throw new IllegalArgumentException("예산은 0 이상이어야 합니다.");
        }
        this.baseLimit = newLimit;
        this.adjustReason = reason;
    }

    public YearMonth getYearMonthParsed() {
        return YearMonth.parse(yearMonth);
    }
}
