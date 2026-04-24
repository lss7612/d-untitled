package com.example.demo.club.untitled.dto;

import java.util.List;

/**
 * 관리자 전용: 특정 월에 책 신청을 한 건도 하지 않은 ACTIVE 멤버 목록.
 * 구조는 {@link MissingSubmittersResponse} (독후감 미제출자) 와 의도적으로 유사하게 맞춘다.
 */
public record UnsubmittedMembersResponse(
    String targetMonth,
    int totalActiveMembers,
    int submittedCount,
    List<UnsubmittedMember> unsubmitted
) {
    public record UnsubmittedMember(Long memberId, String name, String email) {}
}
