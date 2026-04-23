package com.example.demo.club.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "club_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "member_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private ClubRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private MembershipStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 기존 시드/테스트 호환용: 즉시 ACTIVE 상태로 생성 */
    public static ClubMember of(Long clubId, Long memberId, ClubRole role) {
        return of(clubId, memberId, role, MembershipStatus.ACTIVE);
    }

    public static ClubMember of(Long clubId, Long memberId, ClubRole role, MembershipStatus status) {
        ClubMember cm = new ClubMember();
        cm.clubId = clubId;
        cm.memberId = memberId;
        cm.role = role;
        cm.status = status;
        return cm;
    }

    /** 회원이 가입 신청. PENDING + MEMBER 역할. */
    public static ClubMember requestJoin(Long clubId, Long memberId) {
        return of(clubId, memberId, ClubRole.MEMBER, MembershipStatus.PENDING);
    }

    public void changeRole(ClubRole role) {
        this.role = role;
    }

    public void approve() {
        this.status = MembershipStatus.ACTIVE;
    }

    public void reject() {
        this.status = MembershipStatus.REJECTED;
    }

    /** 거절된 신청을 다시 대기 상태로 돌리기 (재신청). */
    public void repending() {
        this.status = MembershipStatus.PENDING;
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public boolean isPending() {
        return status == MembershipStatus.PENDING;
    }

    public enum ClubRole {
        ADMIN, MEMBER
    }

    public enum MembershipStatus {
        PENDING, ACTIVE, REJECTED
    }
}
