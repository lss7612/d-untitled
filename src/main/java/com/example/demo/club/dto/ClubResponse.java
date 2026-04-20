package com.example.demo.club.dto;

import com.example.demo.club.domain.Club;
import com.example.demo.club.domain.ClubMember.ClubRole;

public record ClubResponse(
    Long id,
    String name,
    String description,
    String type,
    ClubRole myRole,
    boolean joined
) {
    public static ClubResponse of(Club club, ClubRole myRole) {
        return new ClubResponse(
            club.getId(),
            club.getName(),
            club.getDescription(),
            club.getType().name(),
            myRole,
            myRole != null
        );
    }
}
