package com.example.demo.club.dto;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.ClubRole;
import com.example.demo.club.domain.ClubMember.MembershipStatus;
import com.example.demo.user.domain.Member;

import java.time.LocalDateTime;

public record ClubMemberResponse(
    Long clubId,
    Long memberId,
    String memberName,
    String memberEmail,
    ClubRole role,
    MembershipStatus status,
    LocalDateTime joinedAt
) {
    public static ClubMemberResponse of(ClubMember cm, Member member) {
        return new ClubMemberResponse(
            cm.getClubId(),
            cm.getMemberId(),
            member != null ? member.getName() : null,
            member != null ? member.getEmail() : null,
            cm.getRole(),
            cm.getStatus(),
            cm.getJoinedAt()
        );
    }
}
