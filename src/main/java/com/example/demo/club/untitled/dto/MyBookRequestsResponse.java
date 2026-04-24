package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.budget.dto.BudgetShareResponse;

import java.util.List;

/**
 * 본인 책 신청 페이지의 배너/리스트 데이터.
 *
 * <p>예산 필드 의미:
 * <ul>
 *   <li>{@code budgetLimit} — 정책 스냅샷 기반 기본 한도 (30,000 / 35,000).</li>
 *   <li>{@code transferIn} — 이번 달 내가 ACCEPTED 로 받은 나눔 총액.</li>
 *   <li>{@code transferOut} — 이번 달 내가 ACCEPTED 로 전달한 나눔 총액.</li>
 *   <li>{@code effectiveLimit} — {@code budgetLimit + transferIn − transferOut}.</li>
 *   <li>{@code budgetUsed} — 내가 신청한 책들의 가격 합계.</li>
 *   <li>{@code budgetRemaining} — {@code effectiveLimit − budgetUsed}.</li>
 * </ul>
 *
 * <p>{@code acceptedShares} 는 배너에 "○○님에게 받은 N원 / ○○님에게 전달한 N원" 뱃지를 그리기 위한 원본.
 */
public record MyBookRequestsResponse(
    String targetMonth,
    int budgetLimit,
    int transferIn,
    int transferOut,
    int effectiveLimit,
    int budgetUsed,
    int budgetRemaining,
    boolean locked,
    List<BookRequestResponse> requests,
    List<BudgetShareResponse> acceptedShares
) {}
