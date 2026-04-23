package com.example.demo.club.dto;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.MembershipStatus;
import com.example.demo.user.domain.Member;

import java.time.LocalDateTime;

public record JoinRequestResponse(
    Long clubId,
    Long memberId,
    String memberName,
    String memberEmail,
    MembershipStatus status,
    LocalDateTime requestedAt
) {
    public static JoinRequestResponse of(ClubMember cm, Member member) {
        return new JoinRequestResponse(
            cm.getClubId(),
            cm.getMemberId(),
            member != null ? member.getName() : null,
            member != null ? member.getEmail() : null,
            cm.getStatus(),
            cm.getJoinedAt()
        );
    }
}
