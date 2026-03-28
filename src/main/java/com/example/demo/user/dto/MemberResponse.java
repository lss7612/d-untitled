package com.example.demo.user.dto;

import com.example.demo.user.domain.Member;

public record MemberResponse(
        Long id,
        String email,
        String name,
        String picture,
        String role
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPicture(),
                member.getRole().name()
        );
    }
}
