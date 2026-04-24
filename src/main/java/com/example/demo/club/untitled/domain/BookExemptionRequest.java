package com.example.demo.club.untitled.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 회원이 보낸 "중복 책에 대한 제한풀기 신청".
 *
 * <p>회원이 책을 신청하다 {@link Book} 카탈로그의 중복에 걸렸을 때 호출해 기록한다.
 * 관리자(혹은 DEVELOPER) 가 {@link Status#APPROVED} 로 처리하면 {@link Book#grantExemption()} 을
 * 함께 호출해 실제로 중복 체크에서 제외시킨다 (책 단위 영구 예외).
 */
@Entity
@Table(
    name = "book_exemption_request",
    indexes = {
        @Index(name = "idx_ber_club_status", columnList = "club_id, status"),
        @Index(name = "idx_ber_book", columnList = "book_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookExemptionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    /** 신청한 회원. */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 신청 사유 (선택). */
    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private Status status;

    @Column(name = "processed_by_member_id")
    private Long processedByMemberId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static BookExemptionRequest of(Long clubId, Long bookId, Long memberId, String reason) {
        BookExemptionRequest r = new BookExemptionRequest();
        r.clubId = clubId;
        r.bookId = bookId;
        r.memberId = memberId;
        r.reason = reason;
        r.status = Status.PENDING;
        return r;
    }

    public void approve(Long adminMemberId) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }
        this.status = Status.APPROVED;
        this.processedByMemberId = adminMemberId;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(Long adminMemberId) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }
        this.status = Status.REJECTED;
        this.processedByMemberId = adminMemberId;
        this.processedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public enum Status { PENDING, APPROVED, REJECTED }
}
