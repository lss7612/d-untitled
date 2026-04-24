package com.example.demo.club.untitled.budget.service;

import com.example.demo.user.domain.Member;

import java.time.YearMonth;

/**
 * 멤버의 특정 월 "기본 예산 한도"를 계산하는 정책.
 * 스냅샷(MemberMonthlyBudget)이 최초 생성될 때 한 번만 호출되며, 이후엔 저장된 값이 사용됨.
 * 정책이 바뀌어도 기존 스냅샷은 불변 → 과거 월 데이터 보존.
 */
public interface BudgetPolicy {
    int computeBaseLimit(Member member, YearMonth yearMonth);
}
