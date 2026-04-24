package com.example.demo.club.untitled.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 클럽·월 단위 "신청 접수 잠금" 플래그.
 *
 * <p>{@code locked=true} 이면 회원의 새 BookRequest 생성이 차단되고,
 * 관리자가 주문 처리(markOrdered)를 진행할 수 있다.
 * {@link com.example.demo.club.untitled.domain.BookRequest} 상태와는 완전히 독립.
 */
@Entity
@Table(
    name = "month_locks",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_month_locks_club_month",
        columnNames = {"club_id", "target_month"}
    ),
    indexes = {
        @Index(name = "idx_month_locks_club", columnList = "club_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MonthLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    /** 'YYYY-MM'. */
    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Column(nullable = false)
    private boolean locked;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static MonthLock of(Long clubId, String targetMonth) {
        MonthLock m = new MonthLock();
        m.clubId = clubId;
        m.targetMonth = targetMonth;
        m.locked = false;
        return m;
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }
}
