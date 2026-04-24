package com.example.demo.club.untitled.budget.dto;

/**
 * 나눔 신청 생성 바디.
 * targetMonth 포맷: "YYYY-MM" (예: "2026-04").
 * amount 는 양의 정수(원). note 는 선택.
 */
public record CreateBudgetShareRequest(
    String targetMonth,
    Long senderId,
    int amount,
    String note
) {
}
