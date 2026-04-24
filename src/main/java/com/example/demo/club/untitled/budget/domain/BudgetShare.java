package com.example.demo.club.untitled.budget.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 유저 간 월별 예산 "이체" 레코드.
 * requester(받는 쪽)가 sender(주는 쪽)에게 amount 만큼 나눔을 요청. sender가 수락 시 양측 한도 재계산에 반영.
 * 라이프사이클: PENDING → {ACCEPTED | REJECTED | CANCELLED}. ACCEPTED 는 불변.
 */
@Entity
@Table(
    name = "budget_share",
    indexes = {
        @Index(name = "idx_bs_req", columnList = "club_id, target_month, requester_id"),
        @Index(name = "idx_bs_sender", columnList = "club_id, target_month, sender_id"),
        @Index(name = "idx_bs_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BudgetShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private BudgetShareStatus status;

    @Column(length = 200)
    private String note;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public static BudgetShare create(Long clubId, YearMonth targetMonth, Long requesterId, Long senderId,
                                     int amount, String note) {
        if (requesterId == null || senderId == null) {
            throw new IllegalArgumentException("요청자/전달자 정보가 필요합니다.");
        }
        if (requesterId.equals(senderId)) {
            throw new IllegalArgumentException("자기 자신에게 나눔 신청할 수 없습니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 양의 정수여야 합니다.");
        }
        BudgetShare s = new BudgetShare();
        s.clubId = clubId;
        s.targetMonth = targetMonth.toString();
        s.requesterId = requesterId;
        s.senderId = senderId;
        s.amount = amount;
        s.note = note;
        s.status = BudgetShareStatus.PENDING;
        return s;
    }

    public void accept() {
        requirePending();
        this.status = BudgetShareStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        requirePending();
        this.status = BudgetShareStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void cancel() {
        requirePending();
        this.status = BudgetShareStatus.CANCELLED;
        this.respondedAt = LocalDateTime.now();
    }

    private void requirePending() {
        if (status != BudgetShareStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 나눔 신청입니다. (" + status + ")");
        }
    }

    public boolean isPending() { return status == BudgetShareStatus.PENDING; }
    public boolean isAccepted() { return status == BudgetShareStatus.ACCEPTED; }

    public YearMonth getTargetYearMonth() {
        return YearMonth.parse(targetMonth);
    }
}
