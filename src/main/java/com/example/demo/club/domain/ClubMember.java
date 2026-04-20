package com.example.demo.club.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
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
    @Column(nullable = false)
    private ClubRole role;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    public static ClubMember of(Long clubId, Long memberId, ClubRole role) {
        ClubMember cm = new ClubMember();
        cm.clubId = clubId;
        cm.memberId = memberId;
        cm.role = role;
        return cm;
    }

    public void changeRole(ClubRole role) {
        this.role = role;
    }

    public enum ClubRole {
        ADMIN, MEMBER
    }
}
