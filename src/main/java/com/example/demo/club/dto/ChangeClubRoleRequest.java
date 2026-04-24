package com.example.demo.club.dto;

import com.example.demo.club.domain.ClubMember.ClubRole;

public record ChangeClubRoleRequest(
    ClubRole role
) {}
