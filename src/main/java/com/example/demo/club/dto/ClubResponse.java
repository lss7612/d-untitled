package com.example.demo.club.dto;

import com.example.demo.club.domain.Club;
import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.ClubRole;
import com.example.demo.club.domain.ClubMember.MembershipStatus;

public record ClubResponse(
    Long id,
    String name,
    String description,
    String type,
    ClubRole myRole,
    boolean joined,
    MembershipStatus joinStatus
) {
    public static ClubResponse of(Club club, ClubMember membership) {
        ClubRole role = membership != null ? membership.getRole() : null;
        MembershipStatus status = membership != null ? membership.getStatus() : null;
        boolean joined = membership != null && membership.isActive();
        return new ClubResponse(
            club.getId(),
            club.getName(),
            club.getDescription(),
            club.getType().name(),
            joined ? role : null,
            joined,
            status
        );
    }
}
