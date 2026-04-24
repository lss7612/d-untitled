package com.example.demo.club.untitled.budget.dto;

/**
 * 관리자 전용: 클럽의 월별 예산 사용 현황 요약.
 * - totalRequestedAmount: 상태 무관 SUM(BookRequest.price) (PENDING/LOCKED/ORDERED/ARRIVED/RECEIVED 전부)
 * - totalBaseLimit: ACTIVE 멤버 전원의 baseLimit 합계 (나눔은 클럽 내부 이전이라 총합 불변이므로 base 로 충분)
 * - usagePercent: totalBaseLimit 이 0 이면 0.0
 */
public record BudgetSummaryResponse(
    String targetMonth,
    int totalRequestedAmount,
    int totalBaseLimit,
    double usagePercent
) {}
