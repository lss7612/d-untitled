package com.example.demo.club.untitled.budget.dto;

import com.example.demo.club.untitled.budget.domain.MemberMonthlyBudget;
import com.example.demo.user.domain.Member;

import java.time.LocalDateTime;

public record MemberBudgetResponse(
    Long memberId,
    String memberName,
    String memberEmail,
    String yearMonth,
    int baseLimit,
    String adjustReason,
    LocalDateTime updatedAt
) {
    public static MemberBudgetResponse of(MemberMonthlyBudget b, Member member) {
        return new MemberBudgetResponse(
            b.getMemberId(),
            member != null ? member.getName() : null,
            member != null ? member.getEmail() : null,
            b.getYearMonth(),
            b.getBaseLimit(),
            b.getAdjustReason(),
            b.getUpdatedAt()
        );
    }
}
