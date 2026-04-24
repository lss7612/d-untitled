package com.example.demo.club.untitled.budget.service;

import com.example.demo.user.domain.Member;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * 무제 독서동호회 기본 정책:
 * - 가입월(Member.createdAt의 YearMonth): 30,000원
 * - 그 다음 월부터: 35,000원
 */
@Component
public class DefaultBudgetPolicy implements BudgetPolicy {

    public static final int FIRST_MONTH_LIMIT = 30_000;
    public static final int REGULAR_LIMIT = 35_000;

    @Override
    public int computeBaseLimit(Member member, YearMonth yearMonth) {
        YearMonth joinMonth = YearMonth.from(member.getCreatedAt());
        return joinMonth.equals(yearMonth) ? FIRST_MONTH_LIMIT : REGULAR_LIMIT;
    }
}
